package com.poketft.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.poketft.overlay.ui.theme.*

/**
 * 전체화면 오버레이 + 최소화 버블 서비스
 *
 * [IME 처리 방식 — 근본 수정]
 * 기존: FLAG_NOT_FOCUSABLE ↔ 제거를 updateViewLayout()으로 토글
 *   → updateViewLayout()은 비동기, focusRequester.requestFocus()는 동기
 *   → 창이 아직 NOT_FOCUSABLE인 상태에서 포커스 요청 → IME 연결 거부 (경쟁 조건)
 *
 * 현재: FLAG_NOT_FOCUSABLE 완전 제거, FLAG_NOT_TOUCH_MODAL만 사용
 *   → 창이 항상 포커스 가능 → IME 연결 상시 유효
 *   → InputMethodManager.showSoftInput / hideSoftInputFromWindow 로만 제어
 *   → 경쟁 조건 원천 차단
 */
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var bubbleView: ComposeView? = null
    private val uiState = OverlayUIState()

    // IME 제어용
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var hideImeRunnable: Runnable? = null
    private var imeRefCount = 0

    // Compose에 필요한 LifecycleOwner
    private val lifecycleOwner = object : SavedStateRegistryOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
        val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry

        init {
            savedStateRegistryController.performAttach()
            savedStateRegistryController.performRestore(null)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        Repo.ensureLoaded(this)
        MyPokemonStore.load(this)

        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        showOverlay()
    }

    /**
     * IME 직접 제어 — 창 플래그 토글 없이 InputMethodManager로만 키보드 표시/숨김
     *
     * 레퍼런스 카운팅: 여러 필드가 동시에 show/hide를 호출해도 안전
     * 디바운스 200ms: 필드 간 이동 시 키보드 깜빡임 방지
     *
     * @param show true → 키보드 표시, false → 200ms 뒤 키보드 숨김
     */
    private fun setKeyboardVisible(show: Boolean) {
        val view = overlayView ?: return
        val imm = getSystemService(INPUT_METHOD_SERVICE)
            as android.view.inputmethod.InputMethodManager

        if (show) {
            imeRefCount++
            // 대기 중인 숨김 취소
            hideImeRunnable?.let { mainHandler.removeCallbacks(it) }
            hideImeRunnable = null
            // 다음 프레임에 실행 — Compose 포커스 처리가 완료된 뒤 IME 요청
            mainHandler.post {
                imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        } else {
            if (imeRefCount > 0) imeRefCount--
            if (imeRefCount == 0) {
                val r = Runnable {
                    if (imeRefCount == 0) {
                        imm.hideSoftInputFromWindow(view.windowToken, 0)
                    }
                }
                hideImeRunnable = r
                mainHandler.postDelayed(r, 200L)
            }
        }
    }

    /** 전체화면 오버레이 표시 */
    private fun showOverlay() {
        removeBubble()
        Repo.ensureLoaded(this)

        // IME 상태 초기화
        hideImeRunnable?.let { mainHandler.removeCallbacks(it) }
        hideImeRunnable = null
        imeRefCount = 0

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // ★ FLAG_NOT_FOCUSABLE 완전 제거
            // FLAG_NOT_TOUCH_MODAL: 창 바깥 터치는 하위 창으로 통과
            //                        + 창은 항상 포커스 가능 → IME 즉시 연결 가능
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).also {
            it.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                PoketftTheme {
                    CalculatorOverlay(
                        state = uiState,
                        onClose = { hideOverlay() },
                        onRequestFocus = { show -> setKeyboardVisible(show) }
                    )
                }
            }
        }

        windowManager.addView(overlayView, params)
    }

    /** 오버레이를 숨기고 버블만 표시 */
    private fun hideOverlay() {
        // 오버레이 닫을 때 키보드 즉시 숨김
        overlayView?.let {
            val imm = getSystemService(INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
        removeOverlay()
        showBubble()
    }

    /** 플로팅 버블 표시 */
    private fun showBubble() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 100
        }

        bubbleView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                BubbleButton { showOverlay() }
            }
        }

        windowManager.addView(bubbleView, params)
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
    }

    private fun removeBubble() {
        bubbleView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        bubbleView = null
    }

    override fun onDestroy() {
        hideImeRunnable?.let { mainHandler.removeCallbacks(it) }
        removeOverlay()
        removeBubble()
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}

/** 최소화 시 표시되는 작은 버블 버튼 */
@Composable
private fun BubbleButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(PokeAccent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("⚔", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

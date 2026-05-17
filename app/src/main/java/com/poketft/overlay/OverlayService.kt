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
 * - 가로 모드 전체화면
 * - 검색 팝업 시 키보드 입력 가능 (FLAG_NOT_FOCUSABLE 토글)
 */
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var bubbleView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private val uiState = OverlayUIState()

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

        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        showOverlay()
    }

    /**
     * 포커스 모드 전환 — 검색 팝업에서 키보드 입력을 받기 위해
     * @param focusable true: 키보드 입력 가능, false: 게임에 터치 통과
     */
    private fun setOverlayFocusable(focusable: Boolean) {
        val view = overlayView ?: return
        val params = overlayParams ?: return
        params.flags = if (focusable) {
            // 키보드 입력 가능 — FLAG_NOT_FOCUSABLE 제거
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        } else {
            // 기본 — 터치가 뒤로 전달되지 않지만 포커스도 안 가져감
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    /** 전체화면 오버레이 표시 */
    private fun showOverlay() {
        removeBubble()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        overlayParams = params

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                PoketftTheme {
                    CalculatorOverlay(
                        state = uiState,
                        onClose = { hideOverlay() },
                        onRequestFocus = { focusable -> setOverlayFocusable(focusable) }
                    )
                }
            }
        }

        windowManager.addView(overlayView, params)
    }

    /** 오버레이를 숨기고 버블만 표시 */
    private fun hideOverlay() {
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
        overlayParams = null
    }

    private fun removeBubble() {
        bubbleView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        bubbleView = null
    }

    override fun onDestroy() {
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

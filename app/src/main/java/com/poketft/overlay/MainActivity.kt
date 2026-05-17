package com.poketft.overlay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poketft.overlay.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // JSON 데이터 로딩
        Repo.load(this)

        setContent {
            PoketftTheme {
                MainScreen()
            }
        }
    }

    @Composable
    private fun MainScreen() {
        var hasPermission by remember {
            mutableStateOf(Settings.canDrawOverlays(this@MainActivity))
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            hasPermission = Settings.canDrawOverlays(this@MainActivity)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PokeBg, PokeSurface)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⚔ POKETFT",
                    color = PokeAccent,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "포켓몬 실전 데미지 계산기",
                    color = PokeTextSec,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "포켓몬 ${Repo.pokemons.size}종 / 기술 ${Repo.movesById.size}개 로드됨",
                    color = PokeTextSec,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (hasPermission) {
                    // 오버레이 시작 버튼
                    Button(
                        onClick = {
                            startService(Intent(this@MainActivity, OverlayService::class.java))
                            finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PokeAccent),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .width(200.dp)
                    ) {
                        Text(
                            "오버레이 시작",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PokeTextPri
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 오버레이 종료 버튼
                    OutlinedButton(
                        onClick = {
                            stopService(Intent(this@MainActivity, OverlayService::class.java))
                        },
                        modifier = Modifier.width(200.dp)
                    ) {
                        Text("오버레이 종료", fontSize = 13.sp, color = PokeTextSec)
                    }
                } else {
                    // 권한 요청
                    Text(
                        text = "오버레이 권한이 필요합니다\n설정에서 '다른 앱 위에 표시'를 허용해주세요",
                        color = PokeRed,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            permissionLauncher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PokeBlue)
                    ) {
                        Text("권한 설정 열기", fontSize = 14.sp, color = PokeTextPri)
                    }
                }
            }
        }
    }
}

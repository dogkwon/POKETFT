package com.poketft.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poketft.overlay.ui.theme.*

/**
 * 랭크(능력치 변화) 조작 팝업 — ±1 단계 조절
 * 스탯: Atk, Def, SpA, SpD, Spe (HP 제외)
 */
@Composable
fun RankPopup(
    panel: PanelState,
    onDismiss: () -> Unit
) {
    val statLabels = listOf("공격", "방어", "특공", "특방", "스피드")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(PokeSurface)
                .clickable { /* 이벤트 소비 */ }
                .padding(16.dp)
                .widthIn(max = 340.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("랭크 변화", color = PokeTextPri, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            statLabels.forEachIndexed { idx, label ->
                val rank = panel.ranks[idx]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(label, color = PokeTextSec, fontSize = 11.sp,
                        modifier = Modifier.width(50.dp))

                    // -1 버튼
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PokeRed.copy(alpha = 0.7f))
                            .clickable { panel.adjustRank(idx, -1) },
                        contentAlignment = Alignment.Center
                    ) { Text("-1", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 현재 랭크
                    Text(
                        text = if (rank >= 0) "+$rank" else "$rank",
                        color = when {
                            rank > 0 -> PokeBlue
                            rank < 0 -> PokeRed
                            else -> PokeTextSec
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(40.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // +1 버튼
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PokeBlue.copy(alpha = 0.7f))
                            .clickable { panel.adjustRank(idx, 1) },
                        contentAlignment = Alignment.Center
                    ) { Text("+1", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 배율 표시
                    val mul = CalcEngine.rankMultiplier(rank)
                    Text("×${"%.2f".format(mul)}", color = PokeGreen, fontSize = 10.sp)
                }
                if (idx < statLabels.lastIndex) Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 초기화 + 닫기 버튼
            Row {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PokeBorder)
                        .clickable { for (i in panel.ranks.indices) panel.ranks[i] = 0 }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) { Text("초기화", color = PokeTextSec, fontSize = 11.sp) }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PokeAccent)
                        .clickable { onDismiss() }
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                ) { Text("닫기", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

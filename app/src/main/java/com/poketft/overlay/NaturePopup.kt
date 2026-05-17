package com.poketft.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * 성격 선택 팝업 — 5×5 격자
 * 행: 상승 스탯 (공격, 방어, 특공, 특방, 스피드)
 * 열: 하락 스탯 (공격, 방어, 특공, 특방, 스피드)
 * 대각선: 무보정 성격
 */
@Composable
fun NaturePopup(
    panel: PanelState,
    onDismiss: () -> Unit
) {
    val labels = NatureData.STAT_LABELS // 공격, 방어, 특공, 특방, 스피드

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
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "성격 선택",
                color = PokeTextPri,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "현재: ${panel.nature.nameKo}",
                color = PokeBlue,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 헤더 행 (하락 스탯 라벨)
            Row {
                // 빈 코너
                Box(modifier = Modifier.size(48.dp, 20.dp))
                labels.forEach { label ->
                    Text(
                        text = "↓$label",
                        color = PokeRed.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 5×5 격자
            NatureData.GRID.forEachIndexed { rowIdx, row ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 행 라벨 (상승 스탯)
                    Text(
                        text = "↑${labels[rowIdx]}",
                        color = PokeBlue.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(48.dp).padding(end = 4.dp)
                    )

                    row.forEachIndexed { colIdx, nature ->
                        val isSelected = panel.nature == nature
                        val isNeutral = nature.isNeutral
                        val bgColor = when {
                            isSelected -> PokeAccent
                            isNeutral -> PokeCard
                            else -> PokeBg
                        }
                        val borderColor = if (isSelected) PokeAccent else PokeBorder

                        Box(
                            modifier = Modifier
                                .padding(1.dp)
                                .size(54.dp, 28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(bgColor)
                                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                                .clickable {
                                    panel.nature = nature
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = nature.nameKo,
                                color = if (isSelected) Color.White else PokeTextPri,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(1.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PokeAccent)
                    .clickable { onDismiss() }
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            ) {
                Text("닫기", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

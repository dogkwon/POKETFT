package com.poketft.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
 * 성격(25종) + 노력치 슬라이더 통합 팝업
 *
 * EV 체계: 0~32 스케일 (총합 66 제한)
 *   - 32+32+2 = 66  ↔  원본 252+252+4 = 508 의 1/8 축소
 *   - 내부 공식: ev * 2  (= ev*8/4, floor 포함)
 */
@Composable
fun StatSetupPopup(
    panel: PanelState,
    onDismiss: () -> Unit
) {
    val statLabels = listOf("H", "A", "B", "C", "D", "S")
    val statFull   = listOf("HP", "공격", "방어", "특공", "특방", "스피드")
    val natureLabels = NatureData.STAT_LABELS

    // 성격 보정 색상
    fun natureBuff(idx: Int): Color = when {
        panel.nature.isNeutral            -> PokeTextPri
        idx == panel.nature.upIndex       -> Color(0xFFFF6B6B)
        idx == panel.nature.downIndex     -> Color(0xFF64B5F6)
        else                              -> PokeTextPri
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PokeSurface)
                .clickable { }
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 타이틀 ────────────────────────────────
            Text("⚙️ Stat 설정", color = PokeTextPri, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "6V 고정 · ${panel.pokemon?.name_ko ?: ""}",
                color = PokeTextSec, fontSize = 9.sp
            )

            Spacer(Modifier.height(10.dp))

            // ── 성격 25종 그리드 ─────────────────────
            Text("성격 (25종)", color = PokeAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))

            Row {
                Box(Modifier.size(40.dp, 16.dp))
                natureLabels.forEach { label ->
                    Text(
                        "↓$label", color = PokeRed.copy(0.8f), fontSize = 7.sp,
                        modifier = Modifier.width(44.dp), textAlign = TextAlign.Center
                    )
                }
            }
            NatureData.GRID.forEachIndexed { rowIdx, row ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "↑${natureLabels[rowIdx]}", color = PokeBlue.copy(0.8f), fontSize = 7.sp,
                        modifier = Modifier.width(40.dp), textAlign = TextAlign.End
                    )
                    row.forEach { nature ->
                        val sel = panel.nature == nature
                        Box(
                            modifier = Modifier
                                .padding(1.dp)
                                .size(42.dp, 22.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (sel) PokeAccent else PokeBg)
                                .border(1.dp, if (sel) PokeAccent else PokeBorder, RoundedCornerShape(3.dp))
                                .clickable { panel.nature = nature },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                nature.nameKo,
                                color = if (sel) Color.White else PokeTextPri,
                                fontSize = 7.sp, maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── 노력치 슬라이더 (0~32 스케일) ────────
            val evTotal = panel.evs.sum()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "노력치 (EV)",
                    color = PokeAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "합계 $evTotal / 66",
                    color = if (evTotal > 66) PokeRed else PokeTextSec,
                    fontSize = 9.sp, fontWeight = FontWeight.Bold
                )
            }
            Text(
                "32+32+2 = 풀투자 기준 · 1단위 = 실제 EV 8",
                color = PokeTextSec, fontSize = 7.sp
            )

            Spacer(Modifier.height(6.dp))

            // 헤더
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("스탯",    color = PokeTextSec, fontSize = 8.sp, modifier = Modifier.width(52.dp))
                Text("EV",     color = PokeTextSec, fontSize = 8.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Spacer(Modifier.weight(1f))
                Text("실수치", color = PokeTextSec, fontSize = 8.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = PokeBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 3.dp))

            statLabels.forEachIndexed { idx, short ->
                val p = panel.pokemon
                val actualStat = p?.let {
                    if (idx == 0) CalcEngine.calcHP(it.stats[0], panel.evs[idx])
                    else CalcEngine.calcStat(it.stats[idx], panel.evs[idx], panel.nature.multiplier(idx))
                }

                // 남은 EV 한도 (총합 66 제한)
                val otherEvs = panel.evs.sumOf { it } - panel.evs[idx]
                val maxEv = minOf(32, 66 - otherEvs).coerceAtLeast(0)

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "$short ${statFull[idx]}",
                            color = natureBuff(if (idx == 0) -1 else idx),
                            fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(52.dp)
                        )
                        Text(
                            "${panel.evs[idx]}",
                            color = PokeAccent,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.Center
                        )
                        Slider(
                            value = panel.evs[idx].toFloat(),
                            onValueChange = { v ->
                                panel.evs[idx] = v.toInt().coerceIn(0, maxEv)
                            },
                            valueRange = 0f..32f,
                            steps = 31,       // 0~32 = 33 단계 → steps = 31
                            modifier = Modifier.weight(1f).height(28.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = PokeAccent,
                                activeTrackColor = PokeAccent,
                                inactiveTrackColor = PokeBorder
                            )
                        )
                        Text(
                            if (actualStat != null) "$actualStat" else "-",
                            color = PokeGreen,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── 빠른 설정 버튼 ──────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                QuickButton("초기화", PokeRed, Modifier.weight(1f)) {
                    for (i in panel.evs.indices) panel.evs[i] = 0
                }
                QuickButton("공/속 32", PokeBlue, Modifier.weight(1f)) {
                    for (i in panel.evs.indices) panel.evs[i] = 0
                    panel.evs[1] = 32   // 공격
                    panel.evs[5] = 32   // 스피드
                    panel.evs[0] = 2    // HP 나머지
                }
                QuickButton("균등(11)", PokeGreen, Modifier.weight(1f)) {
                    for (i in panel.evs.indices) panel.evs[i] = 11
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PokeAccent)
                    .clickable { onDismiss() }
                    .padding(horizontal = 36.dp, vertical = 7.dp)
            ) {
                Text("닫기", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.75f))
            .clickable { onClick() }
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

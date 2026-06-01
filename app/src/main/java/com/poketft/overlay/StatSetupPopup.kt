package com.poketft.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
 * 성격(25종) + 노력치(H~S, ±32) 통합 팝업 — 개체값 6V 고정
 */
@Composable
fun StatSetupPopup(
    panel: PanelState,
    onDismiss: () -> Unit
) {
    val statLabels = listOf("H", "A", "B", "C", "D", "S")
    val statFull = listOf("HP", "공격", "방어", "특공", "특방", "스피드")
    val natureLabels = NatureData.STAT_LABELS

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 480.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PokeSurface)
                .clickable { }
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚙️ Stat 설정", color = PokeTextPri, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("6V 고정 · ${panel.pokemon?.name_ko ?: ""}", color = PokeTextSec, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Text("성격 (25종)", color = PokeAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Box(modifier = Modifier.size(40.dp, 16.dp))
                natureLabels.forEach { label ->
                    Text("↓$label", color = PokeRed.copy(0.8f), fontSize = 7.sp,
                        modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                }
            }
            NatureData.GRID.forEachIndexed { rowIdx, row ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("↑${natureLabels[rowIdx]}", color = PokeBlue.copy(0.8f), fontSize = 7.sp,
                        modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
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
                            Text(nature.nameKo, color = if (sel) Color.White else PokeTextPri,
                                fontSize = 7.sp, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("노력치 (±32)", color = PokeAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            val evTotal = panel.evs.sum()
            Text("합계 $evTotal / 510", color = if (evTotal > 510) PokeRed else PokeTextSec, fontSize = 8.sp)

            statLabels.forEachIndexed { idx, short ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Text("$short", color = PokeTextSec, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(16.dp))
                    Text(statFull[idx], color = PokeTextSec, fontSize = 9.sp, modifier = Modifier.width(36.dp))
                    Box(
                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(3.dp))
                            .background(PokeRed.copy(0.7f))
                            .clickable { panel.adjustEv(idx, -32) },
                        contentAlignment = Alignment.Center
                    ) { Text("-", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    Text("${panel.evs[idx]}", color = PokeTextPri, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                    Box(
                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(3.dp))
                            .background(PokeBlue.copy(0.7f))
                            .clickable { panel.adjustEv(idx, 32) },
                        contentAlignment = Alignment.Center
                    ) { Text("+", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    panel.pokemon?.let { p ->
                        val preview = if (idx == 0) CalcEngine.calcHP(p.stats[0], panel.evs[idx])
                        else CalcEngine.calcStat(p.stats[idx], panel.evs[idx], panel.nature.multiplier(idx))
                        Text("→$preview", color = PokeGreen, fontSize = 8.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(PokeAccent)
                    .clickable { onDismiss() }.padding(horizontal = 28.dp, vertical = 6.dp)
            ) { Text("닫기", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

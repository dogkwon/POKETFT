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
 * 랭크 조절 팝업 — H,A,B,C,D,S (-6 ~ +6)
 */
@Composable
fun RankPopup(
    panel: PanelState,
    onDismiss: () -> Unit
) {
    val short = listOf("H", "A", "B", "C", "D", "S")
    val full = listOf("HP", "공격", "방어", "특공", "특방", "스피드")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(PokeSurface)
                .clickable { }
                .padding(12.dp)
                .widthIn(max = 320.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📊 Rank 조절", color = PokeTextPri, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(panel.pokemon?.name_ko ?: "", color = PokeTextSec, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(8.dp))

            short.forEachIndexed { idx, label ->
                val rank = panel.ranks[idx]
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(label, color = PokeAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(14.dp))
                    Text(full[idx], color = PokeTextSec, fontSize = 9.sp, modifier = Modifier.width(40.dp))
                    Box(
                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(3.dp))
                            .background(PokeRed.copy(0.7f))
                            .clickable { panel.adjustRank(idx, -1) },
                        contentAlignment = Alignment.Center
                    ) { Text("-", color = Color.White, fontSize = 10.sp) }
                    Text(
                        if (rank >= 0) "+$rank" else "$rank",
                        color = when { rank > 0 -> PokeBlue; rank < 0 -> PokeRed; else -> PokeTextSec },
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(32.dp), textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(3.dp))
                            .background(PokeBlue.copy(0.7f))
                            .clickable { panel.adjustRank(idx, 1) },
                        contentAlignment = Alignment.Center
                    ) { Text("+", color = Color.White, fontSize = 10.sp) }
                    Text("×${"%.2f".format(CalcEngine.rankMultiplier(rank))}",
                        color = PokeGreen, fontSize = 8.sp)
                }
                if (idx < short.lastIndex) Spacer(modifier = Modifier.height(3.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(PokeBorder)
                        .clickable { for (i in panel.ranks.indices) panel.ranks[i] = 0 }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) { Text("초기화", color = PokeTextSec, fontSize = 10.sp) }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(PokeAccent)
                        .clickable { onDismiss() }
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                ) { Text("닫기", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

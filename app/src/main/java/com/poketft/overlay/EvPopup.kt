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
 * 노력치(EV) 조작 팝업 — 32씩 증감
 * 스탯: HP, Atk, Def, SpA, SpD, Spe
 */
@Composable
fun EvPopup(
    panel: PanelState,
    onDismiss: () -> Unit
) {
    val statLabels = listOf("HP", "공격", "방어", "특공", "특방", "스피드")

    // 반투명 배경 + 중앙 팝업
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
                .clickable { /* 이벤트 소비 — 뒤로 전달 방지 */ }
                .padding(16.dp)
                .widthIn(max = 340.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 타이틀
            Text(
                text = "노력치 (EV) 설정",
                color = PokeTextPri,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 각 스탯 행
            statLabels.forEachIndexed { idx, label ->
                EvRow(
                    label = label,
                    value = panel.evs[idx],
                    onMinus = { panel.adjustEv(idx, -32) },
                    onPlus = { panel.adjustEv(idx, 32) },
                    panel = panel
                )
                if (idx < statLabels.lastIndex) Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // EV 총합
            val total = panel.evs.sum()
            Text(
                text = "총합: $total / 510",
                color = if (total > 510) PokeRed else PokeTextSec,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 닫기 버튼
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

@Composable
private fun EvRow(label: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit, panel: PanelState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 라벨
        Text(
            text = label,
            color = PokeTextSec,
            fontSize = 11.sp,
            modifier = Modifier.width(50.dp)
        )

        // -32 버튼
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PokeRed.copy(alpha = 0.7f))
                .clickable { onMinus() },
            contentAlignment = Alignment.Center
        ) {
            Text("-32", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 현재 값
        Text(
            text = value.toString(),
            color = PokeTextPri,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // +32 버튼
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PokeBlue.copy(alpha = 0.7f))
                .clickable { onPlus() },
            contentAlignment = Alignment.Center
        ) {
            Text("+32", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 실수치 미리보기
        val pokemon = panel.pokemon
        if (pokemon != null) {
            val actual = if (label == "HP") {
                CalcEngine.calcHP(pokemon.stats[0], value)
            } else {
                val statIdx = listOf("HP", "공격", "방어", "특공", "특방", "스피드").indexOf(label)
                CalcEngine.calcStat(pokemon.stats[statIdx], value, panel.nature.multiplier(statIdx))
            }
            Text(
                text = "→ $actual",
                color = PokeGreen,
                fontSize = 10.sp
            )
        }
    }
}



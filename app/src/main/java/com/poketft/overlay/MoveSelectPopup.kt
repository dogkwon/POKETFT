package com.poketft.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poketft.overlay.ui.theme.*

@Composable
fun MoveSelectPopup(
    panel: PanelState,
    slotIdx: Int,
    onDismiss: () -> Unit
) {
    val pokemon = panel.pokemon ?: run { onDismiss(); return }
    var query by remember { mutableStateOf("") }
    val allMoves = remember(pokemon) {
        pokemon.learnable_moves.mapNotNull { Repo.movesById[it] }
            .filter { it.power > 0 }
            .sortedByDescending { it.power }
    }
    val filtered = remember(query, allMoves) {
        if (query.isBlank()) allMoves
        else allMoves.filter { it.name_ko.contains(query, ignoreCase = true) }
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
                .widthIn(max = 320.dp)
                .heightIn(max = 400.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PokeSurface)
                .clickable { /* consume */ }
                .padding(12.dp)
        ) {
            Text(
                "기술 선택",
                color = PokeTextPri, fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filtered) { move ->
                    val alreadyAdded = panel.assignedMoveIds.contains(move.id)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!alreadyAdded) {
                                    if (slotIdx < panel.assignedMoveIds.size) {
                                        panel.assignedMoveIds[slotIdx] = move.id
                                    } else {
                                        panel.assignedMoveIds.add(move.id)
                                    }
                                    panel.selectedMoveId = move.id
                                    onDismiss()
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        TypeBadge(move.type)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (move.category == "physical") "⚔" else "✦",
                            fontSize = 9.sp, color = PokeTextSec
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            move.name_ko,
                            color = if (alreadyAdded) PokeTextSec.copy(0.5f) else PokeTextPri,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${move.power}",
                            color = if (alreadyAdded) PokeTextSec.copy(0.5f) else PokeAccent,
                            fontSize = 11.sp
                        )
                    }
                    HorizontalDivider(color = PokeBorder.copy(alpha = 0.3f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PokeAccent)
                    .clickable { onDismiss() }
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text("닫기", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

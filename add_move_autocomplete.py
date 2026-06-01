import re
with open('app/src/main/java/com/poketft/overlay/CalculatorOverlay.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_move = """        val moves = remember(moveListKey) { DamageCalculator.panelMoves(panel) }
        CompactDropdown(
            label = "기술",
            value = moves.find { it.id == panel.selectedMoveId }?.let { "${it.name_ko} (${it.power})" }
                ?: if (panel.pokemon != null) "선택" else "포켓몬 먼저",
            enabled = moves.isNotEmpty()
        ) { onDismiss ->
            moves.forEach { move ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TypeBadge(move.type)
                            Spacer(Modifier.width(4.dp))
                            Text(move.name_ko, fontSize = 9.sp, color = PokeTextPri, maxLines = 1)
                        }
                    },
                    onClick = {
                        panel.selectedMoveId = move.id
                        onDismiss()
                    }
                )
            }
        }"""

new_move = """        MoveAutocompleteField(
            panel = panel,
            enabled = panel.pokemon != null
        )"""

content = content.replace(old_move, new_move)

move_component = """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveAutocompleteField(
    panel: PanelState,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    
    val selectedMove = remember(panel.selectedMoveId) { 
        if (panel.selectedMoveId > 0) Repo.movesById[panel.selectedMoveId] else null 
    }
    var query by remember(panel.selectedMoveId, panel.pokemon?.id) { 
        mutableStateOf(selectedMove?.name_ko ?: "") 
    }

    val allMoves = remember(panel.pokemon?.id) {
        panel.pokemon?.let { Repo.getLearnableMoves(it) } ?: emptyList()
    }

    val suggestions = remember(query, allMoves, expanded) {
        if (!expanded) emptyList()
        else if (query.isBlank()) allMoves.take(30)
        else allMoves.filter { it.name_ko.contains(query, ignoreCase = true) }.take(30)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("기술", color = PokeTextSec, fontSize = 8.sp)
        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = { if (enabled) expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = { text ->
                    query = text
                    expanded = true
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(32.dp)
                    .border(1.dp, if (expanded) PokeAccent else PokeBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 9.sp, color = PokeTextPri),
                singleLine = true,
                enabled = enabled,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(PokeAccent),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                        if (query.isEmpty()) {
                            Text(
                                if (enabled) "기술 검색" else "포켓몬 먼저",
                                fontSize = 9.sp,
                                color = PokeTextSec
                            )
                        }
                        innerTextField()
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    }
                }
            )

            ExposedDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 200.dp).background(PokeSurface)
            ) {
                suggestions.forEach { move ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TypeBadge(move.type)
                                Spacer(Modifier.width(4.dp))
                                Text("${move.name_ko} (${move.power})", fontSize = 9.sp, color = PokeTextPri, maxLines = 1)
                            }
                        },
                        onClick = {
                            panel.selectedMoveId = move.id
                            query = move.name_ko
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
"""

content += "\n" + move_component

with open('app/src/main/java/com/poketft/overlay/CalculatorOverlay.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("DONE")

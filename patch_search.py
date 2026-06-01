"""
특성(Ability)과 기술(Move) 모두 실시간 검색 컴포넌트로 교체하는 패치 스크립트
"""
with open('app/src/main/java/com/poketft/overlay/CalculatorOverlay.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# ── 1. 특성 CompactDropdown -> AbilityAutocompleteField ─────────────────────
old_ability = """        val abilities = panel.pokemon?.abilities ?: emptyList()
        CompactDropdown(
            label = "특성",
            value = panel.selectedAbilityKo.ifEmpty { if (panel.pokemon != null) "선택" else "포켓몬 먼저" },
            enabled = abilities.isNotEmpty()
        ) { onDismiss ->
            abilities.forEach { ab ->
                DropdownMenuItem(
                    text = { Text(ab.name_ko, fontSize = 9.sp, color = PokeTextPri) },
                    onClick = {
                        panel.selectedAbility = ab.name_en
                        panel.selectedAbilityKo = ab.name_ko
                        onDismiss()
                    }
                )
            }
        }"""

new_ability = """        AbilityAutocompleteField(
            panel = panel,
            enabled = panel.pokemon != null
        )"""

content = content.replace(old_ability, new_ability)

# ── 2. 기존 MoveAutocompleteField 전체 교체 ──────────────────────────────────
old_move_func = """@OptIn(ExperimentalMaterial3Api::class)
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

new_components = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AbilityAutocompleteField(
    panel: PanelState,
    enabled: Boolean
) {
    val abilities = panel.pokemon?.abilities ?: emptyList()
    var expanded by remember { mutableStateOf(false) }
    var query by remember(panel.pokemon?.id, panel.selectedAbilityKo) {
        mutableStateOf(panel.selectedAbilityKo)
    }

    val suggestions = remember(query, abilities) {
        if (query.isBlank()) abilities
        else abilities.filter { it.name_ko.contains(query, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("특성", color = PokeTextSec, fontSize = 8.sp)
        ExposedDropdownMenuBox(
            expanded = expanded && enabled && abilities.isNotEmpty(),
            onExpandedChange = { if (enabled && abilities.isNotEmpty()) expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = { text ->
                    query = text
                    if (enabled) expanded = true
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
                                if (!enabled) "포켓몬 먼저" else if (abilities.isEmpty()) "특성 없음" else "특성 검색",
                                fontSize = 9.sp, color = PokeTextSec
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
                expanded = expanded && enabled && abilities.isNotEmpty(),
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 180.dp).background(PokeSurface)
            ) {
                suggestions.forEach { ab ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    ab.name_ko,
                                    fontSize = 9.sp,
                                    color = PokeTextPri,
                                    modifier = Modifier.weight(1f)
                                )
                                if (ab.is_hidden) {
                                    Text(
                                        "숨겨진특성",
                                        fontSize = 7.sp,
                                        color = PokeAccent
                                    )
                                }
                            }
                        },
                        onClick = {
                            panel.selectedAbility = ab.name_en
                            panel.selectedAbilityKo = ab.name_ko
                            query = ab.name_ko
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveAutocompleteField(
    panel: PanelState,
    enabled: Boolean
) {
    val allMoves = remember(panel.pokemon?.id) {
        panel.pokemon?.let { Repo.getLearnableMoves(it) } ?: emptyList()
    }

    var expanded by remember { mutableStateOf(false) }
    val selectedMove = remember(panel.selectedMoveId) {
        if (panel.selectedMoveId > 0) Repo.movesById[panel.selectedMoveId] else null
    }
    var query by remember(panel.selectedMoveId, panel.pokemon?.id) {
        mutableStateOf(selectedMove?.name_ko ?: "")
    }

    val suggestions = remember(query, allMoves) {
        if (query.isBlank()) allMoves.take(40)
        else allMoves.filter { it.name_ko.contains(query, ignoreCase = true) }.take(40)
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
                    if (enabled) expanded = true
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
                                if (enabled) "기술명 검색 (${allMoves.size}개)" else "포켓몬 먼저",
                                fontSize = 9.sp, color = PokeTextSec
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
                if (suggestions.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("검색 결과 없음", fontSize = 9.sp, color = PokeTextSec) },
                        onClick = {}
                    )
                } else {
                    suggestions.forEach { move ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TypeBadge(move.type)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "${move.name_ko} (${move.power})",
                                        fontSize = 9.sp, color = PokeTextPri,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        if (move.category == "physical") "물" else "특",
                                        fontSize = 7.sp, color = PokeTextSec
                                    )
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
}
"""

content = content.replace(old_move_func, new_components)

with open('app/src/main/java/com/poketft/overlay/CalculatorOverlay.kt', 'w', encoding='utf-8') as f:
    f.write(content)

# 결과 검증
with open('app/src/main/java/com/poketft/overlay/CalculatorOverlay.kt', 'r', encoding='utf-8') as f:
    result = f.read()

if 'AbilityAutocompleteField' in result:
    print("OK: AbilityAutocompleteField added")
else:
    print("FAIL: AbilityAutocompleteField NOT found")

if 'MoveAutocompleteField' in result and 'suggestions.isEmpty()' in result:
    print("OK: MoveAutocompleteField updated")
else:
    print("FAIL: MoveAutocompleteField NOT updated")

if old_ability in result:
    print("FAIL: old_ability still present")
else:
    print("OK: old_ability removed")

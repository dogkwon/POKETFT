package com.poketft.overlay

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.poketft.overlay.ui.theme.*

private val OverlayBg   = Color(0xCC121212)
private val PanelBg     = Color(0xE6181818)
private val MyDamagePink = Color(0xFFE91E8C)
private val FoeDamageBlue = Color(0xFF4FC3F7)

// ─────────────────────────────────────────────────────────────────────────────
// 최상위 진입점
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CalculatorOverlay(
    state: OverlayUIState,
    onClose: () -> Unit,
    onRequestFocus: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var dbCount by remember { mutableIntStateOf(Repo.pokemonCount) }

    LaunchedEffect(Unit) {
        Repo.ensureLoaded(context.applicationContext)
        dbCount = Repo.pokemonCount
    }

    Box(modifier = Modifier.fillMaxSize().background(OverlayBg)) {
        // 닫기 버튼
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(PokeRed.copy(alpha = 0.85f))
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Text("✕", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
        ) {
            PokemonSidePanel(
                title = "내 포켓몬",
                panel = state.attacker,
                state = state,
                isMine = true,
                dbCount = dbCount,
                onRequestFocus = onRequestFocus,
                modifier = Modifier.weight(3f)
            )
            BattleBoardPanel(state = state, dbCount = dbCount, modifier = Modifier.weight(4f))
            PokemonSidePanel(
                title = "상대",
                panel = state.defender,
                state = state,
                isMine = false,
                dbCount = dbCount,
                onRequestFocus = onRequestFocus,
                modifier = Modifier.weight(3f)
            )
        }

        if (state.showStatPopup) {
            StatSetupPopup(
                panel = state.getPanel(state.statPopupTarget),
                onDismiss = { state.showStatPopup = false }
            )
        }
        if (state.showRankPopup) {
            RankPopup(
                panel = state.getPanel(state.rankPopupTarget),
                onDismiss = { state.showRankPopup = false }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 좌 / 우 패널
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PokemonSidePanel(
    title: String,
    panel: PanelState,
    state: OverlayUIState,
    isMine: Boolean,
    dbCount: Int,
    onRequestFocus: (Boolean) -> Unit,
    modifier: Modifier
) {
    val panelKey = if (isMine) "attacker" else "defender"

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(PanelBg)
            .padding(6.dp)
    ) {
        Text(title, color = PokeAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)

        panel.pokemon?.let { p ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    p.name_ko, color = PokeTextPri, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                p.types.forEach { TypeBadge(it) }
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── 포켓몬 검색 ──────────────────────────────
        PokemonAutocompleteField(
            panel = panel,
            isMine = isMine,
            dbCount = dbCount,
            onRequestFocus = onRequestFocus
        )

        Spacer(Modifier.height(4.dp))

        // ── 특성 검색 ─────────────────────────────────
        AbilitySearchField(panel = panel, onRequestFocus = onRequestFocus)

        Spacer(Modifier.height(3.dp))

        // ── 기술 검색 ─────────────────────────────────
        MoveSearchField(panel = panel, isMine = isMine, onRequestFocus = onRequestFocus)

        Spacer(Modifier.height(3.dp))

        // ── 도구 ─────────────────────────────────────
        SimpleDropdown(
            label = "도구",
            value = BattleContext.labelKo(BattleContext.ATTACKER_HELD, panel.heldItemId),
            options = BattleContext.ATTACKER_HELD.map { it.id to it.labelKo },
            onSelect = { panel.heldItemId = it }
        )

        Spacer(Modifier.height(3.dp))

        // ── 상태이상 ──────────────────────────────────
        SimpleDropdown(
            label = "상태이상",
            value = BattleContext.labelKo(BattleContext.STATUS_CONDITIONS, panel.statusConditionId),
            options = BattleContext.STATUS_CONDITIONS.map { it.id to it.labelKo },
            onSelect = { panel.statusConditionId = it }
        )

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CompactActionButton("⚙️ Stat", PokeYellow, Modifier.weight(1f)) {
                state.statPopupTarget = panelKey
                state.showStatPopup = true
            }
            CompactActionButton("📊 Rank", PokeBlue, Modifier.weight(1f)) {
                state.rankPopupTarget = panelKey
                state.showRankPopup = true
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 포켓몬 자동완성 검색창
//   • 포커스 & 빈 쿼리 → 등록한 포켓몬 바로가기 표시
//   • 타이핑 → 일반 이름 검색 결과 표시
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PokemonAutocompleteField(
    panel: PanelState,
    isMine: Boolean,
    dbCount: Int,
    onRequestFocus: (Boolean) -> Unit
) {
    var query by remember(panel.pokemon?.id) { mutableStateOf(panel.pokemon?.name_ko ?: "") }
    var showSuggestions by remember { mutableStateOf(false) }

    LaunchedEffect(panel.pokemon?.id, panel.pokemon?.name_ko) {
        panel.pokemon?.name_ko?.let { query = it }
    }

    // 일반 검색 결과 (타이핑 시)
    val searchResults = remember(query, dbCount) {
        if (query.isBlank() || dbCount == 0) emptyList()
        else Repo.filterByNameKo(query, limit = 12)
    }

    val showSearch  = showSuggestions && query.isNotBlank() && searchResults.isNotEmpty()
    val focusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (dbCount == 0) {
            Text("DB 없음 — assets 재빌드 필요", color = PokeRed, fontSize = 8.sp)
        }

        // ── 검색 입력창 ──────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .border(
                    1.dp,
                    if (showSuggestions) PokeAccent else PokeBorder,
                    RoundedCornerShape(4.dp)
                )
                .clickable { focusRequester.requestFocus() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = query,
                onValueChange = { text ->
                    query = text
                    showSuggestions = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focus ->
                        onRequestFocus(focus.isFocused)
                        if (focus.isFocused) showSuggestions = true
                    },
                textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, color = PokeTextPri),
                singleLine = true,
                enabled = dbCount > 0,
                cursorBrush = SolidColor(PokeAccent),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                        if (query.isEmpty()) {
                            Text(
                                if (dbCount > 0) "이름 검색 (${dbCount}종)" else "DB 로드 실패",
                                fontSize = 9.sp, color = PokeTextSec
                            )
                        }
                        inner()
                    }
                }
            )
        }


        // ── 일반 검색 결과 (타이핑 시) ──────────────
        if (showSearch) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(2f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E1E1E))
                    .border(1.dp, PokeBorder, RoundedCornerShape(4.dp))
                    .heightIn(max = 120.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                searchResults.forEach { pokemon ->
                    val hasSave = MyPokemonStore.list.any { it.pokemonId == pokemon.id }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val save = MyPokemonStore.list.find { it.pokemonId == pokemon.id }
                                if (isMine && save != null) {
                                    panel.loadFromSave(pokemon, save)
                                } else {
                                    panel.bindPokemonFromSearch(pokemon, resetBuild = true)
                                }
                                query = pokemon.name_ko
                                showSuggestions = false
                                onRequestFocus(false)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            pokemon.name_ko,
                            color = PokeTextPri, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        // 등록된 포켓몬이면 별 표시
                        if (hasSave) {
                            Text("⭐", fontSize = 8.sp)
                            Spacer(Modifier.width(2.dp))
                        }
                        pokemon.types.forEach { TypeBadge(it) }
                    }
                    HorizontalDivider(color = PokeBorder.copy(alpha = 0.25f), thickness = 0.5.dp)
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// 특성 검색창 — BasicTextField 기반, ExposedDropdown 없이 독립 팝업
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AbilitySearchField(panel: PanelState, onRequestFocus: (Boolean) -> Unit = {}) {
    val pokemonId = panel.pokemon?.id
    val abilities = remember(pokemonId) {
        panel.pokemon?.abilities ?: emptyList()
    }

    // 화면 표시용 (선택된 특성명) — 검색 필터와 분리
    val displayText = panel.selectedAbilityKo

    // 목록이 열렸을 때 사용자가 입력하는 검색어
    var searchQuery by remember { mutableStateOf("") }
    var showList by remember(pokemonId) { mutableStateOf(false) }

    // 목록 닫힐 때 searchQuery 초기화
    LaunchedEffect(showList) {
        if (!showList) searchQuery = ""
    }

    val filtered = if (searchQuery.isBlank()) abilities
                   else abilities.filter { it.name_ko.contains(searchQuery, ignoreCase = true) }

    Text("특성", color = PokeTextSec, fontSize = 8.sp)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .border(1.dp, if (showList) PokeAccent else PokeBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (showList) {
                // 목록 열린 상태: 검색 입력
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 24.dp)
                        .onFocusChanged { onRequestFocus(it.isFocused) },
                    textStyle = LocalTextStyle.current.copy(fontSize = 9.sp, color = PokeTextPri),
                    singleLine = true,
                    cursorBrush = SolidColor(PokeAccent),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "특성 검색 (${abilities.size}개)",
                                    fontSize = 9.sp, color = PokeTextSec
                                )
                            }
                            inner()
                        }
                    }
                )
            } else {
                // 목록 닫힌 상태: 선택된 특성명 표시
                Text(
                    text = if (panel.pokemon == null) "포켓몬 먼저"
                           else if (displayText.isEmpty()) "특성 선택"
                           else displayText,
                    fontSize = 9.sp,
                    color = if (panel.pokemon == null) PokeTextSec else PokeTextPri,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 24.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = abilities.isNotEmpty()) {
                        showList = !showList
                        if (!showList) onRequestFocus(false)
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(if (showList) "▲" else "▼", fontSize = 8.sp, color = PokeTextSec)
            }
        }

        if (showList && filtered.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(3f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PokeSurface)
                    .border(1.dp, PokeBorder, RoundedCornerShape(4.dp))
                    .heightIn(max = 120.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                filtered.forEach { ab ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                panel.selectedAbility = ab.name_en
                                panel.selectedAbilityKo = ab.name_ko
                                showList = false
                            }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ab.name_ko, fontSize = 9.sp, color = PokeTextPri, modifier = Modifier.weight(1f))
                        if (ab.is_hidden) {
                            Text("숨겨진특성", fontSize = 7.sp, color = PokeAccent)
                        }
                    }
                    HorizontalDivider(color = PokeBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 기술 선택
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MoveSearchField(
    panel: PanelState,
    isMine: Boolean,
    onRequestFocus: (Boolean) -> Unit = {}
) {
    if (isMine) {
        // ══════════════════════════════════════════════════════════
        // 내 포켓몬: 등록 모드(저장된 4기술 드롭다운) or 학습기술 검색
        // ══════════════════════════════════════════════════════════
        val pokemonId = panel.pokemon?.id
        val isRegisteredMode = panel.assignedMoveIds.isNotEmpty()

        val allMoves = remember(pokemonId, panel.assignedMoveIds.toList()) {
            if (isRegisteredMode)
                panel.assignedMoveIds.mapNotNull { Repo.movesById[it] }
            else
                panel.pokemon?.let { Repo.getLearnableMoves(it) } ?: emptyList()
        }

        var showList by remember(pokemonId) { mutableStateOf(false) }
        var query by remember(pokemonId) { mutableStateOf("") }
        val focusRequester = remember(pokemonId) { FocusRequester() }

        LaunchedEffect(pokemonId) { showList = false; query = "" }

        val selectedName = if (panel.selectedMoveId > 0)
            Repo.movesById[panel.selectedMoveId]?.name_ko ?: "" else ""

        val displayMoves = if (isRegisteredMode || query.isBlank()) allMoves
            else allMoves.filter { it.name_ko.contains(query, ignoreCase = true) }

        Text("기술", color = PokeTextSec, fontSize = 8.sp)
        Column(modifier = Modifier.fillMaxWidth()) {
            // 등록 모드: 드롭다운
            if (isRegisteredMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth().height(32.dp)
                        .border(1.dp, if (showList) PokeAccent else PokeBorder, RoundedCornerShape(4.dp))
                        .clickable(enabled = panel.pokemon != null) { showList = !showList }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (panel.pokemon == null) "포켓몬 먼저"
                               else if (selectedName.isNotEmpty()) selectedName else "기술 선택",
                        fontSize = 9.sp,
                        color = if (selectedName.isNotEmpty() && panel.pokemon != null) PokeTextPri else PokeTextSec,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(if (showList) "▲" else "▼", fontSize = 8.sp, color = PokeTextSec)
                }
            }
            // 일반 검색 모드: 학습 기술 검색창
            else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(32.dp)
                        .border(1.dp, if (showList) PokeAccent else PokeBorder, RoundedCornerShape(4.dp))
                        .clickable(enabled = panel.pokemon != null) {
                            onRequestFocus(true); showList = true; focusRequester.requestFocus()
                        }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { q -> query = q; showList = true },
                        textStyle = LocalTextStyle.current.copy(color = PokeTextPri, fontSize = 9.sp),
                        singleLine = true,
                        enabled = panel.pokemon != null,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        cursorBrush = SolidColor(PokeAccent)
                    )
                    if (query.isEmpty()) {
                        Text(
                            text = if (panel.pokemon == null) "포켓몬 먼저"
                                   else if (selectedName.isNotEmpty()) selectedName else "기술 검색...",
                            color = if (selectedName.isNotEmpty() && panel.pokemon != null) PokeTextPri else PokeTextSec,
                            fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            // 목록
            if (showList && (isRegisteredMode || panel.pokemon != null)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth().zIndex(3f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1A1A1A))
                        .border(1.dp, PokeBorder, RoundedCornerShape(4.dp))
                        .heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (displayMoves.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), Alignment.Center) {
                            Text("검색 결과 없음", color = PokeTextSec, fontSize = 8.sp)
                        }
                    }
                    displayMoves.forEach { move ->
                        val isSelected = move.id == panel.selectedMoveId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) PokeAccent.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    panel.selectedMoveId = move.id
                                    query = move.name_ko
                                    showList = false
                                    onRequestFocus(false)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TypeBadge(move.type)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                move.name_ko, fontSize = 9.sp,
                                color = if (isSelected) PokeAccent else PokeTextPri,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text("${move.power}", fontSize = 8.sp, color = PokeTextSec)
                            Spacer(Modifier.width(4.dp))
                            Text(if (move.category == "physical") "물" else "특", fontSize = 7.sp, color = PokeTextSec)
                        }
                        HorizontalDivider(color = PokeBorder.copy(alpha = 0.2f), thickness = 0.5.dp)
                    }
                }
            }
        }

    } else {
        // ══════════════════════════════════════════════════════════
        // 상대 포켓몬: moves_db 전체 자동완성 (포켓몬 무관 독립)
        // ══════════════════════════════════════════════════════════
        var query by remember { mutableStateOf("") }
        var showList by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        val selectedName = remember(panel.selectedMoveId) {
            if (panel.selectedMoveId > 0) Repo.movesById[panel.selectedMoveId]?.name_ko ?: "" else ""
        }
        val displayMoves = remember(query) { Repo.filterMovesByName(query, limit = 40) }

        Text("기술", color = PokeTextSec, fontSize = 8.sp)
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(32.dp)
                    .border(1.dp, if (showList) PokeAccent else PokeBorder, RoundedCornerShape(4.dp))
                    .clickable {
                        onRequestFocus(true); showList = true; focusRequester.requestFocus()
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { q -> query = q; showList = true },
                    textStyle = LocalTextStyle.current.copy(color = PokeTextPri, fontSize = 9.sp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    cursorBrush = SolidColor(PokeAccent)
                )
                if (query.isEmpty()) {
                    Text(
                        text = if (selectedName.isNotEmpty()) selectedName else "기술 검색...",
                        color = if (selectedName.isNotEmpty()) PokeTextPri else PokeTextSec,
                        fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 공통 기술 목록
        if (showList) {
            Column(
                modifier = Modifier
                    .fillMaxWidth().zIndex(3f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(1.dp, PokeBorder, RoundedCornerShape(4.dp))
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (displayMoves.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), Alignment.Center) {
                        Text("검색 결과 없음", color = PokeTextSec, fontSize = 8.sp)
                    }
                }
                displayMoves.forEach { move ->
                    val isSelected = move.id == panel.selectedMoveId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) PokeAccent.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                panel.selectedMoveId = move.id
                                query = move.name_ko
                                showList = false
                                onRequestFocus(false)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TypeBadge(move.type)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            move.name_ko, fontSize = 9.sp,
                            color = if (isSelected) PokeAccent else PokeTextPri,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text("${move.power}", fontSize = 8.sp, color = PokeTextSec)
                        Spacer(Modifier.width(4.dp))
                        Text(if (move.category == "physical") "물" else "특", fontSize = 7.sp, color = PokeTextSec)
                    }
                    HorizontalDivider(color = PokeBorder.copy(alpha = 0.2f), thickness = 0.5.dp)
                }
            }
        }
    }
}



// ─────────────────────────────────────────────────────────────────────────────

// 단순 드롭다운 (도구 / 상태이상 — 고정 옵션 목록)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SimpleDropdown(
    label: String,
    value: String,
    options: List<Pair<String, String>>,  // id to labelKo
    onSelect: (String) -> Unit
) {
    var showList by remember { mutableStateOf(false) }

    Text(label, color = PokeTextSec, fontSize = 8.sp)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .border(1.dp, if (showList) PokeAccent else PokeBorder, RoundedCornerShape(4.dp))
                .clickable { showList = !showList }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                value,
                fontSize = 9.sp, color = PokeTextPri,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 24.dp)
            )
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                Text(if (showList) "▲" else "▼", fontSize = 8.sp, color = PokeTextSec)
            }
        }

        if (showList) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(3f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PokeSurface)
                    .border(1.dp, PokeBorder, RoundedCornerShape(4.dp))
                    .heightIn(max = 150.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { (id, labelKo) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(id)
                                showList = false
                            }
                            .padding(horizontal = 8.dp, vertical = 7.dp)
                    ) {
                        Text(labelKo, fontSize = 9.sp, color = PokeTextPri)
                    }
                    HorizontalDivider(color = PokeBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 중앙 전투 보드
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BattleBoardPanel(state: OverlayUIState, dbCount: Int, modifier: Modifier) {
    val atkPoke = state.attacker.pokemon
    val defPoke = state.defender.pokemon

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(PanelBg)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EnvironmentDropdownRow(state)
        if (dbCount > 0) {
            Text("DB ${dbCount}종 · 기술 ${Repo.moveCount}개", color = PokeTextSec, fontSize = 7.sp)
        }

        Spacer(Modifier.height(4.dp))

        if (atkPoke != null && defPoke != null) {
            // ── Compose 변경 추적을 위해 모든 동적 상태값을 명시적으로 읽기 ──
            // mutableStateListOf의 각 원소를 직접 읽어야 Compose가 변경을 감지함
            val atkRanks = state.attacker.ranks.toList()   // ranks 변경 → 리컴포지션
            val defRanks = state.defender.ranks.toList()
            val atkEvs   = state.attacker.evs.toList()     // ev 변경 → 리컴포지션
            val defEvs   = state.defender.evs.toList()
            val atkMoveId = state.attacker.selectedMoveId  // 기술 변경 → 리컴포지션
            val defMoveId = state.defender.selectedMoveId
            val weather   = state.weatherId
            val terrain   = state.terrainId
            val wall      = state.globalWallId
            val isCrit    = state.isCritical
            val atkAbility = state.attacker.selectedAbility
            val defAbility = state.defender.selectedAbility
            val atkItem   = state.attacker.heldItemId
            val defItem   = state.defender.heldItemId
            val atkStatus = state.attacker.statusConditionId
            val defStatus = state.defender.statusConditionId
            // 위 변수들이 실제로 사용되지 않아도 읽는 것만으로 스냅샷 의존성 등록됨

            val atkStats = state.attacker.calcActualStats()
            val defStats = state.defender.calcActualStats()

            val myResult = DamageCalculator.compute(
                state.attacker, state.defender, atkPoke, defPoke, atkStats, defStats, state
            )
            val foeResult = DamageCalculator.compute(
                state.defender, state.attacker, defPoke, atkPoke, defStats, atkStats, state
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text("⚔️ MY DAMAGE", color = PokeTextSec, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                if (myResult.hasMove) {
                    Text(
                        "${"%.1f".format(myResult.minPct)}% ~ ${"%.1f".format(myResult.maxPct)}%",
                        color = MyDamagePink, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, lineHeight = 30.sp, maxLines = 1
                    )
                    Text(myResult.koSummary, color = PokeTextSec, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        myResult.moveNameKo, color = PokeTextSec, fontSize = 7.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text("—", color = MyDamagePink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(myResult.koSummary, color = PokeTextSec, fontSize = 8.sp)
                }

                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Color(0xFF444444), thickness = 1.dp)
                Spacer(Modifier.height(4.dp))

                Text("🛡️ FOE DAMAGE", color = PokeTextSec, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                if (foeResult.hasMove) {
                    Text(
                        "${"%.1f".format(foeResult.minPct)}% ~ ${"%.1f".format(foeResult.maxPct)}%",
                        color = FoeDamageBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, lineHeight = 26.sp, maxLines = 1
                    )
                    Text(foeResult.koSummary, color = PokeTextSec, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        foeResult.moveNameKo, color = PokeTextSec, fontSize = 7.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text("—", color = FoeDamageBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(foeResult.koSummary, color = PokeTextSec, fontSize = 8.sp)
                }

            }
        } else {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "양쪽 포켓몬을\n선택하세요",
                    color = PokeTextSec, fontSize = 10.sp, textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 환경 드롭다운 행 (날씨 / 필드 / 벽 / 급소)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EnvironmentDropdownRow(state: OverlayUIState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            EnvMiniDropdown(
                label = "날씨",
                options = BattleContext.WEATHERS,
                selectedId = state.weatherId,
                modifier = Modifier.weight(1f)
            ) { state.weatherId = it }

            EnvMiniDropdown(
                label = "필드",
                options = BattleContext.TERRAINS,
                selectedId = state.terrainId,
                modifier = Modifier.weight(1f)
            ) { state.terrainId = it }

            EnvMiniDropdown(
                label = "벽",
                options = BattleContext.WALLS,
                selectedId = state.globalWallId,
                modifier = Modifier.weight(1f)
            ) { state.globalWallId = it }
        }

        Spacer(Modifier.height(4.dp))

        // ── 내 포켓몬 불러오기 (공격자만) ──────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            RegisteredPokemonLoader(
                state = state,
                isAttacker = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(2f)) // 나머지 공간 채우기
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val atkPoke = state.attacker.pokemon
            val defPoke = state.defender.pokemon
            var atkSpe = 0
            var defSpe = 0
            var showSpeed = false

            if (atkPoke != null && defPoke != null) {
                showSpeed = true
                val atkStats = state.attacker.calcActualStats()
                val defStats = state.defender.calcActualStats()
                atkSpe = CalcEngine.effectiveSpeed(
                    atkStats[5], state.attacker.ranks[5], state.attacker.selectedAbility,
                    state.weatherId, state.attacker.heldItemId, state.attacker.statusConditionId
                )
                defSpe = CalcEngine.effectiveSpeed(
                    defStats[5], state.defender.ranks[5], state.defender.selectedAbility,
                    state.weatherId, state.defender.heldItemId, state.defender.statusConditionId
                )
            }

            // 왼쪽 패널 (내 포켓몬 선공/후공 표시)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                if (showSpeed) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (atkSpe > defSpe) {
                            Text("선공", color = PokeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else if (atkSpe == defSpe) {
                            Text("동속", color = PokeTextSec, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("후공", color = PokeRed, fontSize = 9.sp)
                        }
                        Text("Spe $atkSpe", color = PokeTextSec, fontSize = 7.sp)
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // 중앙 급소 버튼
            val critColor = if (state.isCritical) PokeRed else Color(0xFF2C2C2C)
            val textColor = if (state.isCritical) Color.White else PokeTextSec
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(critColor)
                    .clickable { state.isCritical = !state.isCritical }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (state.isCritical) "💥 급소(Critical) ON" else "💥 급소(Critical) OFF",
                    color = textColor, fontSize = 8.sp, fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            // 오른쪽 패널 (상대 포켓몬 선공/후공 표시)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (showSpeed) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (defSpe > atkSpe) {
                            Text("선공", color = PokeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else if (atkSpe == defSpe) {
                            Text("동속", color = PokeTextSec, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("후공", color = PokeRed, fontSize = 9.sp)
                        }
                        Text("Spe $defSpe", color = PokeTextSec, fontSize = 7.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 환경 미니 드롭다운 (날씨·필드·벽 — 고정 옵션)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EnvMiniDropdown(
    label: String,
    options: List<BattleContext.Option>,
    selectedId: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var showList by remember { mutableStateOf(false) }
    val display = BattleContext.labelKo(options, selectedId).let {
        if (it.length > 5) it.take(4) + "…" else it
    }

    Column(modifier = modifier) {
        Text(label, color = PokeTextSec, fontSize = 7.sp, fontWeight = FontWeight.Bold)

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .border(1.dp, if (showList) PokeAccent else PokeBorder, RoundedCornerShape(4.dp))
                    .clickable { showList = !showList }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(display, fontSize = 8.sp, color = PokeTextPri, modifier = Modifier.padding(end = 16.dp))
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                    Text(if (showList) "▲" else "▼", fontSize = 7.sp, color = PokeTextSec)
                }
            }

            if (showList) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(4f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PokeSurface)
                        .border(1.dp, PokeBorder, RoundedCornerShape(4.dp))
                        .heightIn(max = 120.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    options.forEach { opt ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(opt.id)
                                    showList = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(opt.labelKo, fontSize = 9.sp, color = PokeTextPri)
                        }
                        HorizontalDivider(color = PokeBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 공통 버튼
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CompactActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.75f))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 타입 뱃지
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TypeBadge(type: String) {
    val color = TypeColors[type.lowercase()] ?: Color.Gray
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color)
            .padding(horizontal = 3.dp, vertical = 1.dp)
    ) {
        Text(TypeNames.toKo(type), color = Color.White, fontSize = 6.sp, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 내 포켓몬 불러오기 (환경 패널 하단 전용 버튼)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RegisteredPokemonLoader(state: OverlayUIState, isAttacker: Boolean, modifier: Modifier = Modifier) {
    val panel = if (isAttacker) state.attacker else state.defender
    val opponentPanel = if (isAttacker) state.defender else state.attacker

    val savedPokemons = remember(MyPokemonStore.list.size) {
        MyPokemonStore.list.mapNotNull { save ->
            val base = MyPokemonStore.getBasePokemon(save) ?: return@mapNotNull null
            Pair(base, save)
        }
    }

    var showList by remember { mutableStateOf(false) }

    // 상대방의 현재 속도 계산 (선택 안되어 있으면 0)
    val oppSpe = if (opponentPanel.pokemon != null) {
        val stats = opponentPanel.calcActualStats()
        CalcEngine.effectiveSpeed(
            stats[5], opponentPanel.ranks[5], opponentPanel.selectedAbility,
            state.weatherId, opponentPanel.heldItemId, opponentPanel.statusConditionId
        )
    } else 0

    Column(modifier = modifier) {
        // "등록된 포켓몬" 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(if (showList) PokeAccent else PokeBorder)
                .clickable { showList = !showList }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("등록된 포켓몬", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }

        // 팝업 리스트
        if (showList) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .zIndex(5f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E1E1E))
                    .border(1.dp, PokeAccent.copy(0.5f), RoundedCornerShape(4.dp))
                    .heightIn(max = 160.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (savedPokemons.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("저장된 포켓몬이 없습니다.", color = PokeTextSec, fontSize = 8.sp)
                    }
                } else {
                    savedPokemons.forEach { (pokemon, save) ->
                        val evSummary = save.evs
                            .zip(listOf("H","A","B","C","D","S"))
                            .filter { it.first > 0 }
                            .joinToString(" ") { "${it.second}${it.first}" }
                            
                        // 불러올 포켓몬의 스피드 미리 계산
                        val savedSpe = run {
                            val baseSpe = pokemon.stats.getOrNull(5) ?: 0
                            val rawSpe = CalcEngine.calcStat(baseSpe, save.evs[5], save.toNature().multiplier(5))
                            CalcEngine.effectiveSpeed(
                                baseSpe = rawSpe,
                                rankStage = 0,
                                ability = save.abilityEn,
                                weather = state.weatherId,
                                heldItemId = save.heldItemId,
                                statusConditionId = "none"
                            )
                        }

                        val speedBadge = if (oppSpe > 0) {
                            when {
                                savedSpe > oppSpe -> "선공"
                                savedSpe < oppSpe -> "후공"
                                else -> "동속"
                            }
                        } else ""

                        val speedColor = when(speedBadge) {
                            "선공" -> PokeGreen
                            "후공" -> PokeRed
                            else -> PokeTextSec
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    panel.loadFromSave(pokemon, save)
                                    showList = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    pokemon.name_ko,
                                    color = PokeTextPri, fontSize = 10.sp, fontWeight = FontWeight.Bold
                                )
                                Text(
                                    evSummary,
                                    color = PokeTextSec, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // 스피드 비교 뱃지
                            if (speedBadge.isNotEmpty()) {
                                Text(speedBadge, color = speedColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(6.dp))
                            }
                            
                            Text("불러오기", color = PokeAccent, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = PokeBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

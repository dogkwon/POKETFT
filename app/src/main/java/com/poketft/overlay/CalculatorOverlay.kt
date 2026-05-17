package com.poketft.overlay

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poketft.overlay.ui.theme.*
import kotlin.math.floor

/**
 * 메인 3분할 오버레이 UI (가로 모드 전체화면)
 * [공격자 4] [컨트롤 1] [방어자 4]
 */
@Composable
fun CalculatorOverlay(
    state: OverlayUIState,
    onClose: () -> Unit,
    onRequestFocus: (Boolean) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
    ) {
        // X 버튼 (좌상단)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(PokeRed.copy(alpha = 0.8f))
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Text("✕", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        // 메인 3분할 Row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 36.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
        ) {
            // 공격자 패널
            PokemonPanel(
                label = "공격자",
                panel = state.attacker,
                state = state,
                target = "attacker",
                modifier = Modifier.weight(4f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // 컨트롤 패널
            ControlPanel(
                state = state,
                modifier = Modifier.weight(1.2f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // 방어자 패널
            PokemonPanel(
                label = "방어자",
                panel = state.defender,
                state = state,
                target = "defender",
                modifier = Modifier.weight(4f)
            )
        }

        // ── 팝업 오버레이들 ──
        if (state.showSearchPopup) {
            // 검색 팝업 열림 → 키보드 입력 가능하게
            LaunchedEffect(Unit) { onRequestFocus(true) }
            SearchPopup(state = state, onDismiss = {
                state.showSearchPopup = false
                onRequestFocus(false) // 팝업 닫힘 → 포커스 해제
            })
        }
        if (state.showEvPopup) {
            EvPopup(
                panel = state.getPanel(state.evPopupTarget),
                onDismiss = { state.showEvPopup = false }
            )
        }
        if (state.showNaturePopup) {
            NaturePopup(
                panel = state.getPanel(state.naturePopupTarget),
                onDismiss = { state.showNaturePopup = false }
            )
        }
    }
}

// ── 포켓몬 패널 (공격자/방어자 공용) ────────────────
@Composable
private fun PokemonPanel(
    label: String,
    panel: PanelState,
    state: OverlayUIState,
    target: String,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(PokeCard.copy(alpha = 0.9f))
            .padding(6.dp)
    ) {
        // 헤더: 라벨 + 포켓몬 이름
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = PokeAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = panel.pokemon?.name_ko ?: "선택 없음",
                color = PokeTextPri,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            // 타입 뱃지
            panel.pokemon?.types?.forEach { type ->
                Spacer(modifier = Modifier.width(3.dp))
                TypeBadge(type)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 검색 버튼
        SmallButton(
            text = "🔍 포켓몬 검색",
            color = PokeBlue
        ) {
            state.searchPopupTarget = target
            state.showSearchPopup = true
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 포켓몬 선택됐을 때 상세 표시
        val pokemon = panel.pokemon
        if (pokemon != null) {
            // 스탯 표시 (스크롤 가능)
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                StatsDisplay(panel)

                Spacer(modifier = Modifier.height(4.dp))

                // 기술 목록
                Text("기술:", color = PokeTextSec, fontSize = 9.sp)
                val moves = Repo.getLearnableMoves(pokemon)
                moves.take(8).forEach { move ->
                    MoveRow(move, panel, state)
                }
            }

            // 하단 버튼들
            Row {
                SmallButton("EV", PokeGreen) {
                    state.evPopupTarget = target
                    state.showEvPopup = true
                }
                Spacer(modifier = Modifier.width(4.dp))
                SmallButton("성격: ${panel.nature.nameKo}", PokeYellow) {
                    state.naturePopupTarget = target
                    state.showNaturePopup = true
                }
            }
        }
    }
}

// ── 컨트롤 패널 (가운데) ────────────────
@Composable
private fun ControlPanel(state: OverlayUIState, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(PokeCard.copy(alpha = 0.9f))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // 스왑 버튼
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PokeBlue)
                .clickable { state.swap() }
                .padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("🔄\n스왑", color = Color.White, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        // 초기화 버튼
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PokeRed.copy(alpha = 0.7f))
                .clickable { state.resetAll() }
                .padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("✖\n초기화", color = Color.White, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── 데미지 계산 결과 ──
        DamageResultSection(state)
    }
}

// ── 데미지 결과 표시 ────────────────
@Composable
private fun DamageResultSection(state: OverlayUIState) {
    val atkPoke = state.attacker.pokemon
    val defPoke = state.defender.pokemon
    val moveId = state.attacker.selectedMoveId
    val move = if (moveId > 0) Repo.movesById[moveId] else null

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (atkPoke != null && defPoke != null && move != null) {
            // 공격/방어 실수치 계산
            val atkStats = state.attacker.calcActualStats()
            val defStats = state.defender.calcActualStats()

            // 물리 vs 특수
            val isPhysical = move.category == "physical"
            val atkStatIdx = if (isPhysical) 1 else 3 // Atk or SpA
            val defStatIdx = if (isPhysical) 2 else 4 // Def or SpD
            val atkRankIdx = if (isPhysical) 0 else 2
            val defRankIdx = if (isPhysical) 1 else 3

            val atkVal = floor(atkStats[atkStatIdx] * CalcEngine.rankMultiplier(state.attacker.ranks[atkRankIdx])).toInt()
            val defVal = floor(defStats[defStatIdx] * CalcEngine.rankMultiplier(state.defender.ranks[defRankIdx])).toInt()

            // STAB + 타입 상성
            val stab = CalcEngine.isStab(move.type, atkPoke.types)
            val typeEff = CalcEngine.typeEffectivenessMulti(move.type, defPoke.types)

            val (minDmg, maxDmg) = CalcEngine.calcDamage(move.power, atkVal, defVal, stab, typeEff)

            // HP 대비 퍼센트
            val defHp = defStats[0]
            val minPct = if (defHp > 0) (minDmg * 100.0 / defHp) else 0.0
            val maxPct = if (defHp > 0) (maxDmg * 100.0 / defHp) else 0.0

            // 기술 이름
            Text(move.name_ko, color = PokeTextPri, fontSize = 10.sp, fontWeight = FontWeight.Bold)

            // 타입 상성 표시
            val effText = when {
                typeEff >= 4.0 -> "★ 4배!"
                typeEff >= 2.0 -> "◎ 2배"
                typeEff <= 0.0 -> "✕ 무효"
                typeEff < 1.0 -> "△ 반감"
                else -> "● 등배"
            }
            val effColor = when {
                typeEff >= 2.0 -> PokeGreen
                typeEff <= 0.0 -> Color.Gray
                typeEff < 1.0 -> PokeRed
                else -> PokeTextSec
            }
            Text(effText, color = effColor, fontSize = 9.sp)

            if (stab) {
                Text("자속 1.5x", color = PokeYellow, fontSize = 8.sp)
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 데미지 수치
            Text(
                text = "$minDmg~$maxDmg",
                color = PokeAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${"%.1f".format(minPct)}%~${"%.1f".format(maxPct)}%",
                color = PokeTextSec,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 스피드 비교
            val atkSpe = floor(atkStats[5] * CalcEngine.rankMultiplier(state.attacker.ranks[4])).toInt()
            val defSpe = floor(defStats[5] * CalcEngine.rankMultiplier(state.defender.ranks[4])).toInt()

            val speedColor = when {
                atkSpe > defSpe -> PokeGreen
                atkSpe < defSpe -> PokeRed
                else -> PokeYellow
            }
            val speedText = when {
                atkSpe > defSpe -> "선공 ▶"
                atkSpe < defSpe -> "◀ 후공"
                else -> "동속"
            }
            Text(speedText, color = speedColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("$atkSpe vs $defSpe", color = PokeTextSec, fontSize = 8.sp)
        } else {
            Text("양쪽 포켓몬과\n기술을 선택하세요",
                color = PokeTextSec, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
    }
}

// ── 스탯 표시 ────────────────
@Composable
private fun StatsDisplay(panel: PanelState) {
    val labels = listOf("HP", "Atk", "Def", "SpA", "SpD", "Spe")
    val actuals = panel.calcActualStats()

    labels.forEachIndexed { idx, label ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = PokeTextSec, fontSize = 8.sp, modifier = Modifier.width(28.dp))
            Text(
                "${panel.pokemon?.stats?.get(idx) ?: 0}",
                color = PokeTextSec, fontSize = 8.sp, modifier = Modifier.width(26.dp),
                textAlign = TextAlign.End
            )
            Text(" → ", color = PokeTextSec, fontSize = 8.sp)
            Text(
                "${actuals[idx]}",
                color = PokeTextPri, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.width(30.dp), textAlign = TextAlign.End
            )

            // 랭크 표시 (HP 제외)
            if (idx > 0) {
                val rankIdx = idx - 1
                val rank = panel.ranks[rankIdx]
                Spacer(modifier = Modifier.width(2.dp))
                // - 버튼
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(PokeRed.copy(alpha = 0.5f))
                        .clickable { panel.adjustRank(rankIdx, -1) },
                    contentAlignment = Alignment.Center
                ) { Text("-", color = Color.White, fontSize = 8.sp) }

                Text(
                    text = if (rank >= 0) "+$rank" else "$rank",
                    color = when {
                        rank > 0 -> PokeBlue
                        rank < 0 -> PokeRed
                        else -> PokeTextSec
                    },
                    fontSize = 8.sp,
                    modifier = Modifier.width(22.dp),
                    textAlign = TextAlign.Center
                )

                // + 버튼
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(PokeBlue.copy(alpha = 0.5f))
                        .clickable { panel.adjustRank(rankIdx, 1) },
                    contentAlignment = Alignment.Center
                ) { Text("+", color = Color.White, fontSize = 8.sp) }
            }
        }
    }
}

// ── 기술 행 ────────────────
@Composable
private fun MoveRow(move: Move, panel: PanelState, state: OverlayUIState) {
    val isSelected = panel.selectedMoveId == move.id
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) PokeAccent.copy(alpha = 0.3f) else Color.Transparent)
            .clickable { panel.selectedMoveId = move.id }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // 타입 뱃지
        TypeBadge(move.type)
        Spacer(modifier = Modifier.width(3.dp))
        // 물리/특수
        Text(
            text = if (move.category == "physical") "⚔" else "✦",
            fontSize = 8.sp, color = PokeTextSec
        )
        Spacer(modifier = Modifier.width(2.dp))
        // 기술명
        Text(
            text = move.name_ko,
            color = if (isSelected) PokeTextPri else PokeTextSec,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        // 위력
        Text("${move.power}", color = PokeTextSec, fontSize = 9.sp)
    }
}

// ── 타입 뱃지 ────────────────
@Composable
fun TypeBadge(type: String) {
    val color = TypeColors[type.lowercase()] ?: Color.Gray
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = TypeNames.toKo(type),
            color = Color.White,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── 검색 팝업 ────────────────
@Composable
private fun SearchPopup(state: OverlayUIState, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { Repo.search(query) }

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
                "포켓몬 검색",
                color = PokeTextPri, fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 검색 입력
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("이름 또는 도감번호", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = PokeTextPri),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PokeAccent,
                    unfocusedBorderColor = PokeBorder,
                    cursorColor = PokeAccent
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 결과 리스트
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(results) { pokemon ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                state.getPanel(state.searchPopupTarget).pokemon = pokemon
                                state.getPanel(state.searchPopupTarget).selectedMoveId = -1
                                onDismiss()
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            "#${pokemon.dex_no}",
                            color = PokeTextSec, fontSize = 10.sp,
                            modifier = Modifier.width(36.dp)
                        )
                        Text(
                            pokemon.name_ko,
                            color = PokeTextPri, fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        pokemon.types.forEach { type ->
                            TypeBadge(type)
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                    HorizontalDivider(color = PokeBorder.copy(alpha = 0.3f))
                }
            }
        }
    }
}

// ── 유틸: 작은 버튼 ────────────────
@Composable
private fun SmallButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.7f))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

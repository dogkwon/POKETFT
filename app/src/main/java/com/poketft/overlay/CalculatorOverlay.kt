package com.poketft.overlay

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
            AttackerPanel(
                panel = state.attacker,
                state = state,
                modifier = Modifier.weight(3f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            ControlPanel(
                state = state,
                modifier = Modifier.weight(2f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            DefenderPanel(
                panel = state.defender,
                state = state,
                modifier = Modifier.weight(3f)
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
        if (state.showRankPopup) {
            RankPopup(
                panel = state.getPanel(state.rankPopupTarget),
                onDismiss = { state.showRankPopup = false }
            )
        }
        if (state.showMoveSelectPopup) {
            MoveSelectPopup(
                panel = state.getPanel(state.moveSelectTarget),
                slotIdx = state.moveSelectSlotIdx,
                onDismiss = { state.showMoveSelectPopup = false }
            )
        }
    }
}

// ── 공격자 패널 — 등록된 내 포켓몬만 표시 ────────────────
@Composable
private fun AttackerPanel(
    panel: PanelState,
    state: OverlayUIState,
    modifier: Modifier
) {
    var showMyList by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(PokeCard.copy(alpha = 0.9f))
            .padding(6.dp)
    ) {
        // 헤더
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("공격자", color = PokeAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = panel.pokemon?.name_ko ?: "선택 없음",
                color = PokeTextPri, fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
            panel.pokemon?.types?.forEach { type ->
                Spacer(modifier = Modifier.width(3.dp))
                TypeBadge(type)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 내 포켓몬 선택 버튼
        SmallButton("📋 내 포켓몬", PokeBlue) { showMyList = !showMyList }

        Spacer(modifier = Modifier.height(4.dp))

        if (showMyList) {
            // 등록된 포켓몬 리스트
            if (MyPokemonStore.list.isEmpty()) {
                Text("등록된 포켓몬 없음\n앱에서 먼저 등록하세요",
                    color = PokeTextSec, fontSize = 9.sp)
            } else {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    MyPokemonStore.list.forEach { save ->
                        val base = MyPokemonStore.getBasePokemon(save)
                        if (base != null) {
                            val nature = save.toNature()
                            val moveNames = save.moveIds.take(4)
                                .mapNotNull { Repo.movesById[it]?.name_ko }
                                .joinToString("/")
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (panel.pokemon?.id == base.id) PokeAccent.copy(0.3f)
                                        else PokeSurface
                                    )
                                    .clickable {
                                        panel.loadFromSave(base, save)
                                        showMyList = false
                                    }
                                    .padding(4.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row {
                                        Text(base.name_ko, color = PokeTextPri, fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        base.types.forEach { t ->
                                            TypeBadge(t)
                                            Spacer(modifier = Modifier.width(2.dp))
                                        }
                                    }
                                    Text("${nature.nameKo} | $moveNames",
                                        color = PokeTextSec, fontSize = 8.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val pokemon = panel.pokemon
            if (pokemon != null) {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    panel.assignedMoveIds.mapNotNull { Repo.movesById[it] }.forEach { move ->
                        MoveRow(move, panel, state)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    SmallButton(panel.nature.nameKo, PokeYellow) {}
                    if (pokemon.abilities.isNotEmpty()) {
                        SmallButton(
                            panel.selectedAbilityKo.ifEmpty { pokemon.abilities[0].name_ko },
                            Color(0xFF9B59B6)
                        ) { panel.cycleAbility() }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    SmallButton(
                        "도구:${BattleContext.labelKo(BattleContext.ATTACKER_HELD, panel.heldItemId)}",
                        Color(0xFF16A085)
                    ) { panel.cycleHeldItem(BattleContext.ATTACKER_HELD) }
                    SmallButton(
                        "벽:${BattleContext.labelKo(BattleContext.WALLS, panel.wallId)}",
                        Color(0xFF2980B9)
                    ) { panel.cycleWall() }
                }
            }
        }
    }
}

// ── 방어자 패널 ────────────────
@Composable
private fun DefenderPanel(
    panel: PanelState,
    state: OverlayUIState,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(PokeCard.copy(alpha = 0.9f))
            .padding(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("방어자", color = PokeAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text(panel.pokemon?.name_ko ?: "선택 없음",
                color = PokeTextPri, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            panel.pokemon?.types?.forEach { type ->
                Spacer(modifier = Modifier.width(3.dp)); TypeBadge(type)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        SmallButton("🔍 검색", PokeBlue) {
            state.searchPopupTarget = "defender"; state.showSearchPopup = true
        }
        Spacer(modifier = Modifier.height(4.dp))
        val pokemon = panel.pokemon
        if (pokemon != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (pokemon.abilities.isNotEmpty()) {
                    SmallButton(panel.selectedAbilityKo.ifEmpty { pokemon.abilities[0].name_ko },
                        Color(0xFF9B59B6)) { panel.cycleAbility() }
                }
                SmallButton("도구:${BattleContext.labelKo(BattleContext.DEFENDER_HELD, panel.heldItemId)}",
                    Color(0xFF16A085)) { panel.cycleHeldItem(BattleContext.DEFENDER_HELD) }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Column(modifier = Modifier.weight(1f)) {
                for (slotIdx in 0 until 4) {
                    val moveId = panel.assignedMoveIds.getOrNull(slotIdx)
                    val move = if (moveId != null) Repo.movesById[moveId] else null
                    val isSelected = moveId != null && panel.selectedMoveId == moveId
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) PokeAccent.copy(0.3f) else if (move != null) PokeSurface else PokeBorder.copy(0.3f))
                            .clickable {
                                if (move != null) { panel.selectedMoveId = moveId!! }
                                else { state.moveSelectTarget = "defender"; state.moveSelectSlotIdx = slotIdx; state.showMoveSelectPopup = true }
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        if (move != null) {
                            TypeBadge(move.type); Spacer(Modifier.width(2.dp))
                            Text(if (move.category == "physical") "⚔" else "✦", fontSize = 8.sp, color = PokeTextSec)
                            Spacer(Modifier.width(2.dp))
                            Text(move.name_ko, color = if (isSelected) PokeTextPri else PokeTextSec,
                                fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("${move.power}", color = PokeTextSec, fontSize = 9.sp)
                            Spacer(Modifier.width(2.dp))
                            Box(Modifier.size(14.dp).clip(RoundedCornerShape(2.dp)).background(PokeRed.copy(0.5f))
                                .clickable { if (panel.selectedMoveId == moveId) panel.selectedMoveId = -1; panel.assignedMoveIds.removeAt(slotIdx) },
                                contentAlignment = Alignment.Center) { Text("✕", color = Color.White, fontSize = 7.sp) }
                        } else {
                            Text("+ 기술 추가", color = PokeTextSec, fontSize = 9.sp,
                                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                SmallButton("EV", PokeGreen) { state.evPopupTarget = "defender"; state.showEvPopup = true }
                SmallButton(panel.nature.nameKo, PokeYellow) { state.naturePopupTarget = "defender"; state.showNaturePopup = true }
                SmallButton("Rank", Color(0xFFE67E22)) { state.rankPopupTarget = "defender"; state.showRankPopup = true }
                SmallButton("벽:${BattleContext.labelKo(BattleContext.WALLS, panel.wallId)}",
                    Color(0xFF2980B9)) { panel.cycleWall() }
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
            .verticalScroll(rememberScrollState())
            .clip(RoundedCornerShape(8.dp))
            .background(PokeCard.copy(alpha = 0.9f))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PokeBlue)
                .clickable { state.swap() }
                .padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("🔄\n스왑", color = Color.White, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PokeRed.copy(alpha = 0.7f))
                .clickable { state.resetAll() }
                .padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("✖\n초기화", color = Color.White, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text("환경", color = PokeAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        SmallButton(
            "날씨: ${BattleContext.labelKo(BattleContext.WEATHERS, state.weatherId)}",
            PokeYellow
        ) {
            state.weatherId = BattleContext.nextId(BattleContext.WEATHERS, state.weatherId)
        }
        Spacer(modifier = Modifier.height(2.dp))
        SmallButton(
            "필드: ${BattleContext.labelKo(BattleContext.TERRAINS, state.terrainId)}",
            Color(0xFF8E44AD)
        ) {
            state.terrainId = BattleContext.nextId(BattleContext.TERRAINS, state.terrainId)
        }

        Spacer(modifier = Modifier.height(2.dp))
        SmallButton(
            "급소: ${if (state.isCritical) "ON" else "OFF"}",
            if (state.isCritical) PokeRed else PokeBorder
        ) { state.isCritical = !state.isCritical }

        Spacer(modifier = Modifier.height(6.dp))

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
        // ── 스피드 비교 (양쪽 포켓몬만 있으면 항상 표시) ──
        if (atkPoke != null && defPoke != null) {
            val atkStats = state.attacker.calcActualStats()
            val defStats = state.defender.calcActualStats()

            val atkSpe = CalcEngine.effectiveSpeed(
                atkStats[5], state.attacker.ranks[4], state.attacker.selectedAbility,
                state.weatherId, state.attacker.heldItemId
            )
            val defSpe = CalcEngine.effectiveSpeed(
                defStats[5], state.defender.ranks[4], state.defender.selectedAbility,
                state.weatherId, state.defender.heldItemId
            )

            val fasterName: String
            val speedColor: Color
            when {
                atkSpe > defSpe -> {
                    fasterName = atkPoke.name_ko
                    speedColor = PokeGreen
                }
                defSpe > atkSpe -> {
                    fasterName = defPoke.name_ko
                    speedColor = PokeGreen
                }
                else -> {
                    fasterName = ""
                    speedColor = PokeYellow
                }
            }

            val speedLabel = if (fasterName.isEmpty()) "동속" else "$fasterName 선공"
            Text(speedLabel, color = speedColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("${atkPoke.name_ko} $atkSpe vs $defSpe ${defPoke.name_ko}",
                color = PokeTextSec, fontSize = 7.sp)

            Spacer(modifier = Modifier.height(6.dp))

            // ── 데미지 계산 (기술도 선택됐을 때) ──
            if (move != null) {
                val isPhysical = move.category == "physical"
                val atkStatIdx = if (isPhysical) 1 else 3
                val defStatIdx = if (isPhysical) 2 else 4
                val atkRankIdx = if (isPhysical) 0 else 2
                val defRankIdx = if (isPhysical) 1 else 3

                val atkRankForCalc = if (state.isCritical) {
                    state.attacker.ranks[atkRankIdx].coerceAtLeast(0)
                } else { state.attacker.ranks[atkRankIdx] }
                val defRankForCalc = if (state.isCritical) {
                    state.defender.ranks[defRankIdx].coerceAtMost(0)
                } else { state.defender.ranks[defRankIdx] }

                val atkRanked = floor(atkStats[atkStatIdx] * CalcEngine.rankMultiplier(atkRankForCalc)).toInt()
                val defRanked = floor(defStats[defStatIdx] * CalcEngine.rankMultiplier(defRankForCalc)).toInt()

                val atkVal = CalcEngine.adjustedAttackStat(
                    atkRanked, isPhysical, state.attacker.heldItemId,
                    state.attacker.selectedAbility, state.weatherId
                )
                val defVal = CalcEngine.adjustedDefenseStat(
                    defRanked, isPhysical, defPoke.types, state.weatherId, state.defender.heldItemId
                )

                val stab = CalcEngine.isStab(move.type, atkPoke.types)
                var typeEff = CalcEngine.typeEffectivenessMulti(move.type, defPoke.types)

                // 특성: 면역 체크
                val defAb = state.defender.selectedAbility
                val atkAb = state.attacker.selectedAbility
                val isImmune = CalcEngine.isAbilityImmune(defAb, move.type)

                val envMul = CalcEngine.environmentDamageMultiplier(
                    state.weatherId, state.terrainId, move.type, atkAb
                )
                val lifeOrbMul = if (state.attacker.heldItemId == "life-orb") 1.3 else 1.0

                val criticalMul = if (state.isCritical) 1.5 else 1.0
                val defWall = state.defender.wallId
                val wallMul = when {
                    state.isCritical -> 1.0
                    isPhysical && defWall in listOf("reflect", "aurora-veil") -> 0.5
                    !isPhysical && defWall in listOf("light-screen", "aurora-veil") -> 0.5
                    else -> 1.0
                }

                if (isImmune) {
                    // 특성 면역
                    Text(move.name_ko, color = PokeTextPri, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("✕ 특성 면역", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("(${state.defender.selectedAbilityKo})",
                        color = PokeTextSec, fontSize = 8.sp)
                } else {
                    // 특성 타입 보정
                    typeEff *= CalcEngine.abilityTypeModifier(defAb, move.type)
                    val expertMul =
                        if (state.attacker.heldItemId == "expert-belt" && typeEff > 1.0) 1.2 else 1.0

                    val (minDmg, maxDmg) = CalcEngine.calcDamage(
                        move.power, atkVal, defVal, stab, typeEff,
                        atkAbility = atkAb, defAbility = defAb,
                        moveType = move.type,
                        isPhysical = isPhysical,
                        environmentMul = envMul,
                        expertBeltMul = expertMul,
                        lifeOrbMul = lifeOrbMul,
                        criticalMul = criticalMul,
                        wallMul = wallMul
                    )

                    val defHp = defStats[0]
                    val minPct = if (defHp > 0) (minDmg * 100.0 / defHp) else 0.0
                    val maxPct = if (defHp > 0) (maxDmg * 100.0 / defHp) else 0.0

                    // 기술 이름
                    Text(move.name_ko, color = PokeTextPri, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                    // 타입 상성
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

                    if (stab || atkAb in listOf("protean", "libero")) {
                        val stabLabel = if (atkAb == "adaptability") "자속 2.0x" else "자속 1.5x"
                        Text(stabLabel, color = PokeYellow, fontSize = 8.sp)
                    }
                    if (state.isCritical) {
                        Text("급소! 1.5x", color = PokeRed, fontSize = 8.sp)
                    }
                    if (defAb == "multiscale" && !state.isCritical) {
                        Text("멀티스케일 0.5x", color = PokeYellow, fontSize = 8.sp)
                    }
                    if (wallMul < 1.0) {
                        val wallLabel = when (defWall) {
                            "reflect" -> "리플렉터 0.5x"
                            "light-screen" -> "빛의장막 0.5x"
                            "aurora-veil" -> "오로라베일 0.5x"
                            else -> "벽 0.5x"
                        }
                        Text(wallLabel, color = PokeBlue, fontSize = 8.sp)
                    }
                    if (envMul != 1.0) {
                        Text("날씨·필드 ×${"%.2f".format(envMul)}", color = PokeYellow, fontSize = 8.sp)
                    }
                    if (expertMul > 1.0) {
                        Text("고집스카프 1.2x", color = PokeYellow, fontSize = 8.sp)
                    }
                    if (lifeOrbMul > 1.0) {
                        Text("생명구슬 1.3x", color = PokeYellow, fontSize = 8.sp)
                    }

                    Spacer(modifier = Modifier.height(2.dp))

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
                }
            } else {
                Text("기술을 선택하세요",
                    color = PokeTextSec, fontSize = 9.sp, textAlign = TextAlign.Center)
            }
        } else {
            Text("양쪽 포켓몬을\n선택하세요",
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
                                val p = state.getPanel(state.searchPopupTarget)
                                val resetBuild = state.searchPopupTarget == "defender"
                                p.selectPokemon(pokemon, resetBuild = resetBuild)
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

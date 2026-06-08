package com.poketft.overlay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 한쪽 패널(공격자 or 방어자)의 상태
 */
class PanelState {
    var pokemon by mutableStateOf<Pokemon?>(null)
    var nature by mutableStateOf(NatureData.NEUTRAL)
    var selectedAbility by mutableStateOf("")   // 선택된 특성 (영어명, CalcEngine용)
    var selectedAbilityKo by mutableStateOf("") // 선택된 특성 (한국어명, UI 표시용)
    val evs = mutableStateListOf(0, 0, 0, 0, 0, 0)       // HP,Atk,Def,SpA,SpD,Spe
    val ranks = mutableStateListOf(0, 0, 0, 0, 0, 0)       // HP,Atk,Def,SpA,SpD,Spe
    var selectedMoveId by mutableIntStateOf(-1)
    val assignedMoveIds = mutableStateListOf<Int>()
    /** 장착 도구 id (`BattleContext.ATTACKER_HELD` / `DEFENDER_HELD`) */
    var heldItemId by mutableStateOf("none")
    var wallId by mutableStateOf("none")
    /** 타입 부스트 도구 (목탄·전기구슬 등) */
    var typeBoostHeldId by mutableStateOf("none")
    var statusConditionId by mutableStateOf("none")

    fun reset() {
        pokemon = null
        nature = NatureData.NEUTRAL
        selectedAbility = ""
        selectedAbilityKo = ""
        heldItemId = "none"
        wallId = "none"
        typeBoostHeldId = "none"
        statusConditionId = "none"
        for (i in evs.indices) evs[i] = 0
        for (i in ranks.indices) ranks[i] = 0
        assignedMoveIds.clear()
        selectedMoveId = -1
    }

    /** 검색/선택 시 종족값·특성 바인딩. 기술 선택은 독립적으로 유지 */
    fun bindPokemonFromSearch(p: Pokemon, resetBuild: Boolean = false) {
        pokemon = p
        if (resetBuild) {
            nature = NatureData.NEUTRAL
            for (i in evs.indices) evs[i] = 0
            for (i in ranks.indices) ranks[i] = 0
            heldItemId = "none"
            typeBoostHeldId = "none"
        }
        applyDefaultAbility(p)
        // selectedMoveId는 건드리지 않음 — 기술 선택은 MoveSearchField가 독립 관리
    }

    /** @deprecated bindPokemonFromSearch 사용 */
    fun selectPokemon(p: Pokemon, resetBuild: Boolean = false) =
        bindPokemonFromSearch(p, resetBuild)

    /** 등록된 내 포켓몬 빌드 로드 — EV/성격/특성/기술/도구 전체 복원, 랭크는 0으로 */
    fun loadFromSave(p: Pokemon, save: MyPokemonSave) {
        pokemon = p
        nature = save.toNature()
        for (i in save.evs.indices) {
            if (i < evs.size) evs[i] = save.evs[i]
        }
        for (i in ranks.indices) ranks[i] = 0

        // 기술 복원
        if (save.moveIds.isNotEmpty()) {
            assignedMoveIds.clear()
            assignedMoveIds.addAll(save.moveIds)
            selectedMoveId = save.moveIds.firstOrNull() ?: -1
        }
        // save.moveIds가 비어있으면 selectedMoveId 그대로 유지 (독립 선택 방식)

        // 특성 복원
        if (save.abilityEn.isNotEmpty()) {
            selectedAbility = save.abilityEn
            selectedAbilityKo = save.abilityKo
        } else {
            applyDefaultAbility(p)
        }

        // 도구 복원 (null-safe: 구버전 저장 데이터 호환)
        heldItemId      = (save.heldItemId      ?: "none").ifBlank { "none" }
        typeBoostHeldId = (save.typeBoostHeldId ?: "none").ifBlank { "none" }
    }

    fun cycleAbility() {
        val p = pokemon ?: return
        if (p.abilities.isEmpty()) return
        val currentIdx = p.abilities.indexOfFirst { it.name_en == selectedAbility }.let {
            if (it < 0) 0 else it
        }
        val next = p.abilities[(currentIdx + 1) % p.abilities.size]
        selectedAbility = next.name_en
        selectedAbilityKo = next.name_ko
    }

    private fun applyDefaultAbility(p: Pokemon) {
        if (p.abilities.isNotEmpty()) {
            selectedAbility = p.abilities[0].name_en
            selectedAbilityKo = p.abilities[0].name_ko
        } else {
            selectedAbility = ""
            selectedAbilityKo = ""
        }
    }

    fun cycleHeldItem(options: List<BattleContext.Option>) {
        heldItemId = BattleContext.nextId(options, heldItemId)
    }

    fun adjustEv(statIdx: Int, delta: Int) {
        evs[statIdx] = (evs[statIdx] + delta).coerceIn(0, 32)
    }

    fun adjustRank(statIdx: Int, delta: Int) {
        ranks[statIdx] = (ranks[statIdx] + delta).coerceIn(-6, 6)
    }

    /** 계산된 실수치 반환 [HP, Atk, Def, SpA, SpD, Spe] */
    fun calcActualStats(): IntArray {
        val p = pokemon ?: return IntArray(6)
        val result = IntArray(6)
        result[0] = CalcEngine.calcHP(p.stats[0], evs[0])
        for (i in 1..5) {
            result[i] = CalcEngine.calcStat(p.stats[i], evs[i], nature.multiplier(i))
        }
        return result
    }
}

/**
 * 전체 오버레이 상태
 */
class OverlayUIState {
    val attacker = PanelState()
    val defender = PanelState()
    var isOverlayVisible by mutableStateOf(true)

    /** 전역 날씨·지형 (VGC 규약 근사) */
    var weatherId by mutableStateOf("none")
    var terrainId by mutableStateOf("none")
    /** 중앙 패널 벽 설정 — 방어 측 데미지 계산에 적용 */
    var globalWallId by mutableStateOf("none")
    var isCritical by mutableStateOf(false)

    var showStatPopup by mutableStateOf(false)
    var statPopupTarget by mutableStateOf("attacker")
    var showRankPopup by mutableStateOf(false)
    var rankPopupTarget by mutableStateOf("attacker")

    fun getPanel(target: String): PanelState =
        if (target == "attacker") attacker else defender

    /** 공격자 ↔ 방어자 스왑 */
    fun swap() {
        val tempPoke = attacker.pokemon
        attacker.pokemon = defender.pokemon
        defender.pokemon = tempPoke

        val tempNature = attacker.nature
        attacker.nature = defender.nature
        defender.nature = tempNature

        // 특성 교환
        val tempAb = attacker.selectedAbility
        val tempAbKo = attacker.selectedAbilityKo
        attacker.selectedAbility = defender.selectedAbility
        attacker.selectedAbilityKo = defender.selectedAbilityKo
        defender.selectedAbility = tempAb
        defender.selectedAbilityKo = tempAbKo

        val tempEvs = attacker.evs.toList()
        for (i in attacker.evs.indices) {
            attacker.evs[i] = defender.evs[i]
            defender.evs[i] = tempEvs[i]
        }

        val tempRanks = attacker.ranks.toList()
        for (i in attacker.ranks.indices) {
            attacker.ranks[i] = defender.ranks[i]
            defender.ranks[i] = tempRanks[i]
        }

        val tempMove = attacker.selectedMoveId
        attacker.selectedMoveId = defender.selectedMoveId
        defender.selectedMoveId = tempMove

        val tempAssignedMoves = attacker.assignedMoveIds.toList()
        attacker.assignedMoveIds.clear()
        attacker.assignedMoveIds.addAll(defender.assignedMoveIds)
        defender.assignedMoveIds.clear()
        defender.assignedMoveIds.addAll(tempAssignedMoves)

        val tempItem = attacker.heldItemId
        attacker.heldItemId = defender.heldItemId
        defender.heldItemId = tempItem

        val tempWall = attacker.wallId
        attacker.wallId = defender.wallId
        defender.wallId = tempWall

        val tempBoost = attacker.typeBoostHeldId
        attacker.typeBoostHeldId = defender.typeBoostHeldId
        defender.typeBoostHeldId = tempBoost

        val tempStatus = attacker.statusConditionId
        attacker.statusConditionId = defender.statusConditionId
        defender.statusConditionId = tempStatus
    }

    fun resetAll() {
        weatherId = "none"
        terrainId = "none"
        globalWallId = "none"
        isCritical = false
        attacker.reset()
        defender.reset()
    }
}

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
    val ranks = mutableStateListOf(0, 0, 0, 0, 0)          // Atk,Def,SpA,SpD,Spe
    var selectedMoveId by mutableIntStateOf(-1)
    /** 장착 도구 id (`BattleContext.ATTACKER_HELD` / `DEFENDER_HELD`) */
    var heldItemId by mutableStateOf("none")

    fun reset() {
        pokemon = null
        nature = NatureData.NEUTRAL
        selectedAbility = ""
        selectedAbilityKo = ""
        heldItemId = "none"
        for (i in evs.indices) evs[i] = 0
        for (i in ranks.indices) ranks[i] = 0
        selectedMoveId = -1
    }

    /** 포켓몬 선택 — 첫 번째 특성 기본 선택, resetBuild 시 EV/랭크/성격 초기화 */
    fun selectPokemon(p: Pokemon, resetBuild: Boolean = false) {
        pokemon = p
        if (resetBuild) {
            nature = NatureData.NEUTRAL
            for (i in evs.indices) evs[i] = 0
            for (i in ranks.indices) ranks[i] = 0
            selectedMoveId = -1
            heldItemId = "none"
        }
        applyDefaultAbility(p)
    }

    /** 등록된 내 포켓몬 빌드 로드 (저장된 EV/성격/특성, 랭크는 0으로) */
    fun loadFromSave(p: Pokemon, save: MyPokemonSave) {
        pokemon = p
        nature = save.toNature()
        for (i in save.evs.indices) {
            if (i < evs.size) evs[i] = save.evs[i]
        }
        for (i in ranks.indices) ranks[i] = 0
        selectedMoveId = save.moveIds.firstOrNull() ?: -1
        if (save.abilityEn.isNotEmpty()) {
            selectedAbility = save.abilityEn
            selectedAbilityKo = save.abilityKo
        } else {
            applyDefaultAbility(p)
        }
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
        evs[statIdx] = (evs[statIdx] + delta).coerceIn(0, 252)
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

    var showEvPopup by mutableStateOf(false)
    var evPopupTarget by mutableStateOf("attacker")
    var showNaturePopup by mutableStateOf(false)
    var naturePopupTarget by mutableStateOf("attacker")
    var showSearchPopup by mutableStateOf(false)
    var searchPopupTarget by mutableStateOf("attacker")

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

        val tempItem = attacker.heldItemId
        attacker.heldItemId = defender.heldItemId
        defender.heldItemId = tempItem
    }

    fun resetAll() {
        weatherId = "none"
        terrainId = "none"
        attacker.reset()
        defender.reset()
    }
}

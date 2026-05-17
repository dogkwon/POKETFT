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
    val evs = mutableStateListOf(0, 0, 0, 0, 0, 0)       // HP,Atk,Def,SpA,SpD,Spe
    val ranks = mutableStateListOf(0, 0, 0, 0, 0)          // Atk,Def,SpA,SpD,Spe
    var selectedMoveId by mutableIntStateOf(-1)

    fun reset() {
        pokemon = null
        nature = NatureData.NEUTRAL
        for (i in evs.indices) evs[i] = 0
        for (i in ranks.indices) ranks[i] = 0
        selectedMoveId = -1
    }

    /** EV를 32씩 증감 (0~252 범위) */
    fun adjustEv(statIdx: Int, delta: Int) {
        val newVal = (evs[statIdx] + delta).coerceIn(0, 252)
        evs[statIdx] = newVal
    }

    /** 랭크 증감 (-6~+6 범위) */
    fun adjustRank(statIdx: Int, delta: Int) {
        val newVal = (ranks[statIdx] + delta).coerceIn(-6, 6)
        ranks[statIdx] = newVal
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

    // 팝업 상태
    var showEvPopup by mutableStateOf(false)
    var evPopupTarget by mutableStateOf("attacker") // "attacker" or "defender"
    var showNaturePopup by mutableStateOf(false)
    var naturePopupTarget by mutableStateOf("attacker")
    var showSearchPopup by mutableStateOf(false)
    var searchPopupTarget by mutableStateOf("attacker")

    fun getPanel(target: String): PanelState =
        if (target == "attacker") attacker else defender

    /** 공격자 ↔ 방어자 스왑 */
    fun swap() {
        // 포켓몬 교환
        val tempPoke = attacker.pokemon
        attacker.pokemon = defender.pokemon
        defender.pokemon = tempPoke

        // 성격 교환
        val tempNature = attacker.nature
        attacker.nature = defender.nature
        defender.nature = tempNature

        // EV 교환
        val tempEvs = attacker.evs.toList()
        for (i in attacker.evs.indices) {
            attacker.evs[i] = defender.evs[i]
            defender.evs[i] = tempEvs[i]
        }

        // 랭크 교환
        val tempRanks = attacker.ranks.toList()
        for (i in attacker.ranks.indices) {
            attacker.ranks[i] = defender.ranks[i]
            defender.ranks[i] = tempRanks[i]
        }

        // 선택 기술 교환
        val tempMove = attacker.selectedMoveId
        attacker.selectedMoveId = defender.selectedMoveId
        defender.selectedMoveId = tempMove
    }

    fun resetAll() {
        attacker.reset()
        defender.reset()
    }
}

package com.poketft.overlay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PanelState {
    var pokemon by mutableStateOf<Pokemon?>(null)
    var nature by mutableStateOf(NatureData.NEUTRAL)
    var selectedAbility by mutableStateOf("")
    var selectedAbilityKo by mutableStateOf("")
    val evs = mutableStateListOf(0, 0, 0, 0, 0, 0)
    val ranks = mutableStateListOf(0, 0, 0, 0, 0)
    var selectedMoveId by mutableIntStateOf(-1)
    var heldItemId by mutableStateOf("none")
    var wallId by mutableStateOf("none")
    val assignedMoveIds = mutableStateListOf<Int>()

    fun reset() {
        pokemon = null
        nature = NatureData.NEUTRAL
        selectedAbility = ""
        selectedAbilityKo = ""
        heldItemId = "none"
        wallId = "none"
        for (i in evs.indices) evs[i] = 0
        for (i in ranks.indices) ranks[i] = 0
        selectedMoveId = -1
        assignedMoveIds.clear()
    }

    fun selectPokemon(p: Pokemon, resetBuild: Boolean = false) {
        pokemon = p
        if (resetBuild) {
            nature = NatureData.NEUTRAL
            for (i in evs.indices) evs[i] = 0
            for (i in ranks.indices) ranks[i] = 0
            selectedMoveId = -1
            heldItemId = "none"
            wallId = "none"
            assignedMoveIds.clear()
        }
        applyDefaultAbility(p)
    }

    fun loadFromSave(p: Pokemon, save: MyPokemonSave) {
        pokemon = p
        nature = save.toNature()
        for (i in save.evs.indices) {
            if (i < evs.size) evs[i] = save.evs[i]
        }
        for (i in ranks.indices) ranks[i] = 0
        wallId = "none"
        assignedMoveIds.clear()
        assignedMoveIds.addAll(save.moveIds)
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

    fun cycleWall() {
        wallId = BattleContext.nextId(BattleContext.WALLS, wallId)
    }

    fun adjustEv(statIdx: Int, delta: Int) {
        evs[statIdx] = (evs[statIdx] + delta).coerceIn(0, 252)
    }

    fun adjustRank(statIdx: Int, delta: Int) {
        ranks[statIdx] = (ranks[statIdx] + delta).coerceIn(-6, 6)
    }

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

class OverlayUIState {
    val attacker = PanelState()
    val defender = PanelState()
    var isOverlayVisible by mutableStateOf(true)

    var weatherId by mutableStateOf("none")
    var terrainId by mutableStateOf("none")
    var isCritical by mutableStateOf(false)

    var showEvPopup by mutableStateOf(false)
    var evPopupTarget by mutableStateOf("attacker")
    var showNaturePopup by mutableStateOf(false)
    var naturePopupTarget by mutableStateOf("attacker")
    var showSearchPopup by mutableStateOf(false)
    var searchPopupTarget by mutableStateOf("attacker")
    var showRankPopup by mutableStateOf(false)
    var rankPopupTarget by mutableStateOf("attacker")
    var showMoveSelectPopup by mutableStateOf(false)
    var moveSelectTarget by mutableStateOf("defender")
    var moveSelectSlotIdx by mutableIntStateOf(0)

    fun getPanel(target: String): PanelState =
        if (target == "attacker") attacker else defender

    fun swap() {
        val tempPoke = attacker.pokemon
        attacker.pokemon = defender.pokemon
        defender.pokemon = tempPoke

        val tempNature = attacker.nature
        attacker.nature = defender.nature
        defender.nature = tempNature

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

        val tempWall = attacker.wallId
        attacker.wallId = defender.wallId
        defender.wallId = tempWall

        val tempMoves = attacker.assignedMoveIds.toList()
        attacker.assignedMoveIds.clear()
        attacker.assignedMoveIds.addAll(defender.assignedMoveIds)
        defender.assignedMoveIds.clear()
        defender.assignedMoveIds.addAll(tempMoves)
    }

    fun resetAll() {
        weatherId = "none"
        terrainId = "none"
        isCritical = false
        attacker.reset()
        defender.reset()
    }
}

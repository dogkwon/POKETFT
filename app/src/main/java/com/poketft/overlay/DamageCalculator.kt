package com.poketft.overlay

import kotlin.math.ceil
import kotlin.math.floor

/** 쌍방향 데미지 계산 결과 */
data class DamageResult(
    val minDmg: Int,
    val maxDmg: Int,
    val minPct: Double,
    val maxPct: Double,
    val koSummary: String,
    val moveNameKo: String,
    val hasMove: Boolean
) {
    companion object {
        val EMPTY = DamageResult(0, 0, 0.0, 0.0, "기술 선택", "", false)
    }
}

object DamageCalculator {

    fun panelMoves(panel: PanelState): List<Move> {
        val p = panel.pokemon ?: return emptyList()
        return if (panel.assignedMoveIds.isNotEmpty()) {
            panel.assignedMoveIds.mapNotNull { Repo.movesById[it] }
        } else {
            Repo.getLearnableMoves(p).take(12)
        }
    }

    fun compute(
        atkPanel: PanelState,
        defPanel: PanelState,
        atkPoke: Pokemon,
        defPoke: Pokemon,
        atkStats: IntArray,
        defStats: IntArray,
        state: OverlayUIState
    ): DamageResult {
        val moveId = atkPanel.selectedMoveId
        val move = if (moveId > 0) Repo.movesById[moveId] else null
        if (move == null) return DamageResult.EMPTY.copy(koSummary = "기술 선택")

        val isPhysical = move.category == "physical"
        var atkStatIdx = if (isPhysical) 1 else 3
        val defStatIdx = if (isPhysical) 2 else 4
        var atkRankIdx = if (isPhysical) 0 else 2
        val defRankIdx = if (isPhysical) 1 else 3

        if (move.name_ko == "바디프레스") {
            atkStatIdx = 2 // 방어 스탯
            atkRankIdx = 1 // 방어 랭크
        }

        val atkRankForCalc = if (state.isCritical) {
            atkPanel.ranks[atkRankIdx].coerceAtLeast(0)
        } else atkPanel.ranks[atkRankIdx]
        val defRankForCalc = if (state.isCritical) {
            defPanel.ranks[defRankIdx].coerceAtMost(0)
        } else defPanel.ranks[defRankIdx]

        val atkRanked = floor(atkStats[atkStatIdx] * CalcEngine.rankMultiplier(atkRankForCalc)).toInt()
        val defRanked = floor(defStats[defStatIdx] * CalcEngine.rankMultiplier(defRankForCalc)).toInt()

        val atkVal = CalcEngine.adjustedAttackStat(
            atkRanked, isPhysical, atkPanel.heldItemId,
            atkPanel.selectedAbility, state.weatherId, atkPanel.statusConditionId
        )
        val defVal = CalcEngine.adjustedDefenseStat(
            defRanked, isPhysical, defPoke.types, state.weatherId, defPanel.heldItemId
        )

        val defAb = defPanel.selectedAbility
        val atkAb = atkPanel.selectedAbility
        if (CalcEngine.isAbilityImmune(defAb, move.type)) {
            return DamageResult(
                0, 0, 0.0, 0.0,
                "특성 면역",
                move.name_ko,
                true
            )
        }

        val stab = CalcEngine.isStab(move.type, atkPoke.types)
        var typeEff = CalcEngine.typeEffectivenessMulti(move.type, defPoke.types)
        typeEff *= CalcEngine.abilityTypeModifier(defAb, move.type)

        val envMul = CalcEngine.environmentDamageMultiplier(
            state.weatherId, state.terrainId, move.type, atkAb
        )
        val lifeOrbMul = if (atkPanel.heldItemId == "life-orb") 1.3 else 1.0
        val expertMul = if (atkPanel.heldItemId == "expert-belt" && typeEff > 1.0) 1.2 else 1.0
        val criticalMul = if (state.isCritical) 1.5 else 1.0
        val defWall = state.globalWallId
        val wallMul = when {
            state.isCritical -> 1.0
            isPhysical && defWall in listOf("reflect", "aurora-veil") -> 0.5
            !isPhysical && defWall in listOf("light-screen", "aurora-veil") -> 0.5
            else -> 1.0
        }
        val typeBoostMul = BattleContext.typeBoostDamageMultiplier(
            atkPanel.heldItemId, move.type, atkPoke.name_ko
        )

        // 화상 상태이상 데미지 반감 (물리 공격이면서 공격자 특성이 근성이 아니고 바디프레스가 아닐 때)
        val burnMul = if (atkPanel.statusConditionId == "brn" && isPhysical && atkAb != "guts" && move.name_ko != "바디프레스") 0.5 else 1.0

        val (minDmg, maxDmg) = CalcEngine.calcDamage(
            move.power, atkVal, defVal, stab, typeEff,
            atkAbility = atkAb, defAbility = defAb, moveType = move.type,
            isPhysical = isPhysical, isContact = move.is_contact, environmentMul = envMul,
            expertBeltMul = expertMul, lifeOrbMul = lifeOrbMul,
            typeBoostMul = typeBoostMul,
            criticalMul = criticalMul, wallMul = wallMul,
            burnMul = burnMul
        )

        val hp = defStats[0]
        val minPct = if (hp > 0) minDmg * 100.0 / hp else 0.0
        val maxPct = if (hp > 0) maxDmg * 100.0 / hp else 0.0

        val koMin = if (minDmg > 0) ceil(hp.toDouble() / minDmg).toInt() else 99
        val koMax = if (maxDmg > 0) ceil(hp.toDouble() / maxDmg).toInt() else 99
        val koSummary = when {
            minDmg <= 0 -> "데미지 없음"
            koMax <= 1 -> "확정 1타"
            koMin == koMax -> "확정 ${koMin}타"
            else -> "난수 ${koMax}~${koMin}타"
        }

        return DamageResult(
            minDmg, maxDmg, minPct, maxPct, koSummary, move.name_ko, true
        )
    }
}

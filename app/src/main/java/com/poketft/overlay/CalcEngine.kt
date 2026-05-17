package com.poketft.overlay

import kotlin.math.floor

/**
 * 포켓몬 실수치 / 데미지 / 타입상성 계산 엔진
 * - 레벨 50 고정
 * - 개체값(IV) 31 (6V) 고정
 */
object CalcEngine {

    private const val LV = 50
    private const val IV = 31

    // ── 실수치 계산 ──────────────────────────────────────

    /** HP 실수치 */
    fun calcHP(base: Int, ev: Int): Int {
        return (base * 2 + IV + ev / 4) * LV / 100 + LV + 10
    }

    /** HP 외 스탯 실수치 (성격 보정 포함) */
    fun calcStat(base: Int, ev: Int, natureMul: Double): Int {
        val raw = (base * 2 + IV + ev / 4) * LV / 100 + 5
        return floor(raw * natureMul).toInt()
    }

    // ── 랭크 보정 ──────────────────────────────────────

    /** 랭크 변화 배율 (-6 ~ +6) */
    fun rankMultiplier(stage: Int): Double {
        return if (stage >= 0) (2.0 + stage) / 2.0
        else 2.0 / (2.0 + (-stage))
    }

    // ── 데미지 계산 ──────────────────────────────────────

    /**
     * 데미지 계산
     * @param power   기술 위력
     * @param attack  공격 실수치 (랭크 보정 적용 후)
     * @param defense 방어 실수치 (랭크 보정 적용 후)
     * @param stab    자속 보정 여부
     * @param typeEff 타입 상성 배율
     * @return Pair(최소 데미지, 최대 데미지) — 난수 85%~100%
     */
    fun calcDamage(
        power: Int, attack: Int, defense: Int,
        stab: Boolean, typeEff: Double
    ): Pair<Int, Int> {
        if (power <= 0 || defense <= 0) return 0 to 0
        val base = (22.0 * power * attack.toDouble() / defense) / 50.0 + 2.0
        val stabMul = if (stab) 1.5 else 1.0
        val total = base * stabMul * typeEff
        val minDmg = floor(total * 0.85).toInt().coerceAtLeast(1)
        val maxDmg = floor(total).toInt().coerceAtLeast(1)
        return minDmg to maxDmg
    }

    // ── 타입 상성 ──────────────────────────────────────

    /** 단일 타입 상성 배율 */
    fun typeEffectiveness(atkType: String, defType: String): Double {
        return TYPE_CHART[atkType.lowercase()]?.get(defType.lowercase()) ?: 1.0
    }

    /** 복합 타입 상성 배율 */
    fun typeEffectivenessMulti(atkType: String, defTypes: List<String>): Double {
        var mul = 1.0
        for (dt in defTypes) {
            mul *= typeEffectiveness(atkType, dt)
        }
        return mul
    }

    /** 자속 보정(STAB) 여부 확인 */
    fun isStab(moveType: String, pokemonTypes: List<String>): Boolean {
        return pokemonTypes.any { it.equals(moveType, ignoreCase = true) }
    }

    // ── 18×18 타입 상성 차트 ──────────────────────────────
    // 1.0=보통, 2.0=효과좋음, 0.5=효과별로, 0.0=효과없음
    private val TYPE_CHART: Map<String, Map<String, Double>> = mapOf(
        "normal" to mapOf("rock" to 0.5, "ghost" to 0.0, "steel" to 0.5),
        "fire" to mapOf(
            "fire" to 0.5, "water" to 0.5, "grass" to 2.0, "ice" to 2.0,
            "bug" to 2.0, "rock" to 0.5, "dragon" to 0.5, "steel" to 2.0
        ),
        "water" to mapOf(
            "fire" to 2.0, "water" to 0.5, "grass" to 0.5, "ground" to 2.0,
            "rock" to 2.0, "dragon" to 0.5
        ),
        "grass" to mapOf(
            "fire" to 0.5, "water" to 2.0, "grass" to 0.5, "poison" to 0.5,
            "ground" to 2.0, "flying" to 0.5, "bug" to 0.5, "rock" to 2.0,
            "dragon" to 0.5, "steel" to 0.5
        ),
        "electric" to mapOf(
            "water" to 2.0, "grass" to 0.5, "electric" to 0.5, "ground" to 0.0,
            "flying" to 2.0, "dragon" to 0.5
        ),
        "ice" to mapOf(
            "fire" to 0.5, "water" to 0.5, "grass" to 2.0, "ice" to 0.5,
            "ground" to 2.0, "flying" to 2.0, "dragon" to 2.0, "steel" to 0.5
        ),
        "fighting" to mapOf(
            "normal" to 2.0, "ice" to 2.0, "poison" to 0.5, "flying" to 0.5,
            "psychic" to 0.5, "bug" to 0.5, "rock" to 2.0, "ghost" to 0.0,
            "dark" to 2.0, "steel" to 2.0, "fairy" to 0.5
        ),
        "poison" to mapOf(
            "grass" to 2.0, "poison" to 0.5, "ground" to 0.5, "rock" to 0.5,
            "ghost" to 0.5, "steel" to 0.0, "fairy" to 2.0
        ),
        "ground" to mapOf(
            "fire" to 2.0, "grass" to 0.5, "electric" to 2.0, "poison" to 2.0,
            "flying" to 0.0, "bug" to 0.5, "rock" to 2.0, "steel" to 2.0
        ),
        "flying" to mapOf(
            "grass" to 2.0, "electric" to 0.5, "fighting" to 2.0, "bug" to 2.0,
            "rock" to 0.5, "steel" to 0.5
        ),
        "psychic" to mapOf(
            "fighting" to 2.0, "poison" to 2.0, "psychic" to 0.5, "dark" to 0.0,
            "steel" to 0.5
        ),
        "bug" to mapOf(
            "fire" to 0.5, "grass" to 2.0, "fighting" to 0.5, "poison" to 0.5,
            "flying" to 0.5, "psychic" to 2.0, "ghost" to 0.5, "dark" to 2.0,
            "steel" to 0.5, "fairy" to 0.5
        ),
        "rock" to mapOf(
            "fire" to 2.0, "ice" to 2.0, "fighting" to 0.5, "ground" to 0.5,
            "flying" to 2.0, "bug" to 2.0, "steel" to 0.5
        ),
        "ghost" to mapOf(
            "normal" to 0.0, "psychic" to 2.0, "ghost" to 2.0, "dark" to 0.5
        ),
        "dragon" to mapOf(
            "dragon" to 2.0, "steel" to 0.5, "fairy" to 0.0
        ),
        "dark" to mapOf(
            "fighting" to 0.5, "psychic" to 2.0, "ghost" to 2.0, "dark" to 0.5,
            "fairy" to 0.5
        ),
        "steel" to mapOf(
            "fire" to 0.5, "water" to 0.5, "electric" to 0.5, "ice" to 2.0,
            "rock" to 2.0, "steel" to 0.5, "fairy" to 2.0
        ),
        "fairy" to mapOf(
            "fire" to 0.5, "fighting" to 2.0, "poison" to 0.5, "dragon" to 2.0,
            "dark" to 2.0, "steel" to 0.5
        )
    )
}

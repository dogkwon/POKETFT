package com.poketft.overlay

import kotlin.math.floor

/**
 * 포켓몬 실수치 / 데미지 / 타입상성 / 특성 효과 계산 엔진
 * - 레벨 50 고정, IV 31 (6V) 고정
 */
object CalcEngine {

    private const val LV = 50
    private const val IV = 31

    // ── 실수치 계산 ──────────────────────────────────────

    fun calcHP(base: Int, ev: Int): Int {
        return (base * 2 + IV + ev / 4) * LV / 100 + LV + 10
    }

    fun calcStat(base: Int, ev: Int, natureMul: Double): Int {
        val raw = (base * 2 + IV + ev / 4) * LV / 100 + 5
        return floor(raw * natureMul).toInt()
    }

    // ── 랭크 보정 ──────────────────────────────────────

    fun rankMultiplier(stage: Int): Double {
        return if (stage >= 0) (2.0 + stage) / 2.0
        else 2.0 / (2.0 + (-stage))
    }

    /** 랭크 보정 + 스피드 관련 특성·날씨·도구가 적용된 실질 스피드 */
    fun effectiveSpeed(
        baseSpe: Int,
        rankStage: Int,
        ability: String,
        weather: String,
        heldItemId: String
    ): Int {
        var spe = floor(baseSpe * rankMultiplier(rankStage)).toInt()
        when (ability) {
            "slow-start" -> spe = floor(spe * 0.5).toInt()
            "swift-swim" -> if (weather == "rain") spe *= 2
            "chlorophyll" -> if (weather == "sun") spe *= 2
            "sand-rush" -> if (weather == "sand") spe *= 2
            "slush-rush" -> if (weather == "snow" || weather == "hail") spe *= 2
            "unburden", "quick-feet" -> spe = floor(spe * 1.5).toInt()
        }
        if (heldItemId == "choice-scarf") spe = floor(spe * 1.5).toInt()
        if (heldItemId == "iron-ball") spe = floor(spe * 0.5).toInt()
        return spe
    }

    /** 날씨·지형·모래력 등 환경에 의한 기술 피해 배율 */
    fun environmentDamageMultiplier(
        weather: String,
        terrain: String,
        moveType: String,
        atkAbility: String
    ): Double {
        var m = 1.0
        when (weather) {
            "sun" -> when (moveType) {
                "fire" -> m *= 1.5
                "water" -> m *= 0.5
            }
            "rain" -> when (moveType) {
                "water" -> m *= 1.5
                "fire" -> m *= 0.5
            }
        }
        when (terrain) {
            "electric" -> if (moveType == "electric") m *= 1.3
            "grassy" -> if (moveType == "grass") m *= 1.3
            "psychic" -> if (moveType == "psychic") m *= 1.3
            "misty" -> {
                if (moveType == "fairy") m *= 1.3
                if (moveType == "dragon") m *= 0.5
            }
        }
        if (weather == "sand" && atkAbility == "sand-force" &&
            moveType in listOf("rock", "ground", "steel")
        ) m *= 1.3
        return m
    }

    /** 랭크 반영 후 공격 측 실질 공격/특공 (도구·대낮쾌청 등) */
    fun adjustedAttackStat(
        atkWithRank: Int,
        isPhysical: Boolean,
        heldItemId: String,
        atkAbility: String,
        weather: String
    ): Int {
        var v = atkWithRank.toDouble()
        when (heldItemId) {
            "choice-band" -> if (isPhysical) v *= 1.5
            "choice-specs" -> if (!isPhysical) v *= 1.5
            "muscle-band" -> if (isPhysical) v *= 1.1
            "wise-glasses" -> if (!isPhysical) v *= 1.1
        }
        if (!isPhysical && atkAbility == "solar-power" && weather == "sun") v *= 1.5
        return floor(v).toInt().coerceAtLeast(1)
    }

    /** 랭크 반영 후 방어 측 실질 방어/특방 (날씨·돌격조끼 등) */
    fun adjustedDefenseStat(
        defWithRank: Int,
        isPhysical: Boolean,
        defTypes: List<String>,
        weather: String,
        defenderHeldId: String
    ): Int {
        var v = defWithRank.toDouble()
        if (!isPhysical && weather == "sand" &&
            defTypes.any { it.equals("rock", ignoreCase = true) }
        ) v *= 1.5
        if (isPhysical && (weather == "snow" || weather == "hail") &&
            defTypes.any { it.equals("ice", ignoreCase = true) }
        ) v *= 1.5
        if (!isPhysical && defenderHeldId == "assault-vest") v *= 1.5
        return floor(v).toInt().coerceAtLeast(1)
    }

    // ── 데미지 계산 (특성 효과 포함) ──────────────────────────

    /**
     * @param power      기술 위력
     * @param attack     공격 실수치 (랭크 보정 후)
     * @param defense    방어 실수치 (랭크 보정 후)
     * @param stab       자속 보정
     * @param typeEff    타입 상성 배율
     * @param atkAbility 공격자 특성 (영어명)
     * @param defAbility 방어자 특성 (영어명)
     * @param moveType   기술 타입 (특성 판정용)
     */
    fun calcDamage(
        power: Int, attack: Int, defense: Int,
        stab: Boolean, typeEff: Double,
        atkAbility: String = "", defAbility: String = "",
        moveType: String = "",
        isPhysical: Boolean = true,
        environmentMul: Double = 1.0,
        expertBeltMul: Double = 1.0,
        lifeOrbMul: Double = 1.0
    ): Pair<Int, Int> {
        if (power <= 0 || defense <= 0) return 0 to 0

        // 기본 데미지
        val base = (22.0 * power * attack.toDouble() / defense) / 50.0 + 2.0

        // STAB 배율 — 적응력(Adaptability): 2.0x, 천의무봉(Protean/Libero): 항상 STAB
        val stabMul = when {
            atkAbility in listOf("adaptability") && stab -> 2.0
            atkAbility in listOf("protean", "libero") -> 1.5
            stab -> 1.5
            else -> 1.0
        }

        // 타입 상성 보정 특성
        var effTypeEff = typeEff
        // 하드록/필터(Solid Rock/Filter): 효과 좋은 공격 0.75배
        if (defAbility in listOf("solid-rock", "filter") && typeEff > 1.0) {
            effTypeEff *= 0.75
        }
        // 틴트드렌즈(Tinted Lens): 효과 별로 2배
        if (atkAbility == "tinted-lens" && typeEff < 1.0 && typeEff > 0.0) {
            effTypeEff *= 2.0
        }

        // 공격력 보정 특성
        var atkMul = 1.0
        // 테크니션(Technician): 위력 60 이하 1.5배
        if (atkAbility == "technician" && power <= 60) atkMul *= 1.5
        // 근성(Guts): 공격 1.5배 (상태이상 가정)
        // → UI에서 토글로 별도 처리 가능
        // 철주먹(Iron Fist): 펀치 기술 1.2배
        if (atkAbility == "iron-fist") atkMul *= 1.2
        // 이판사판(Reckless): 반동 기술 1.2배
        if (atkAbility == "reckless") atkMul *= 1.2
        // 메가런처(Mega Launcher): 파동 기술 1.5배
        if (atkAbility == "mega-launcher") atkMul *= 1.5

        // 방어 보정 특성
        var defMul = 1.0
        // 두꺼운지방(Thick Fat): 불꽃/얼음 피해 0.5배
        if (defAbility == "thick-fat" && moveType in listOf("fire", "ice")) {
            defMul *= 0.5
        }
        // 모피코트(Fur Coat): 물리 방어 2배 → 데미지 0.5배
        if (defAbility == "fur-coat" && isPhysical) defMul *= 0.5
        // 멀티스케일(Multiscale): 풀체력 0.5배 (오버레이는 풀HP 가정)
        if (defAbility == "multiscale") defMul *= 0.5

        val total = base * stabMul * effTypeEff * atkMul * defMul *
            environmentMul * expertBeltMul * lifeOrbMul
        val minDmg = floor(total * 0.85).toInt().coerceAtLeast(1)
        val maxDmg = floor(total).toInt().coerceAtLeast(1)
        return minDmg to maxDmg
    }

    // ── 특성에 의한 타입 면역 체크 ──────────────────────────

    /**
     * 방어자 특성에 의한 타입 면역 여부
     * @return true면 면역 (데미지 0)
     */
    fun isAbilityImmune(defAbility: String, moveType: String): Boolean {
        return when (defAbility) {
            "levitate" -> moveType == "ground"
            "volt-absorb", "lightning-rod", "motor-drive" -> moveType == "electric"
            "water-absorb", "storm-drain" -> moveType == "water"
            "flash-fire" -> moveType == "fire"
            "dry-skin" -> moveType == "water"
            "sap-sipper" -> moveType == "grass"
            else -> false
        }
    }

    /**
     * 방어자 특성에 의한 추가 타입 상성 보정
     * (면역 외: 건조피부의 불꽃 1.25배 등)
     */
    fun abilityTypeModifier(defAbility: String, moveType: String): Double {
        if (defAbility == "dry-skin" && moveType == "fire") return 1.25
        if (defAbility == "fluffy" && moveType == "fire") return 2.0
        return 1.0
    }

    // ── 자속 보정(STAB) ──────────────────────────────────

    fun isStab(moveType: String, pokemonTypes: List<String>): Boolean {
        return pokemonTypes.any { it.equals(moveType, ignoreCase = true) }
    }

    // ── 타입 상성 ──────────────────────────────────────

    fun typeEffectiveness(atkType: String, defType: String): Double {
        return TYPE_CHART[atkType.lowercase()]?.get(defType.lowercase()) ?: 1.0
    }

    fun typeEffectivenessMulti(atkType: String, defTypes: List<String>): Double {
        var mul = 1.0
        for (dt in defTypes) mul *= typeEffectiveness(atkType, dt)
        return mul
    }

    // ── 18×18 타입 상성 차트 ──────────────────────────────
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

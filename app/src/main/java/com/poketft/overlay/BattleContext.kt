package com.poketft.overlay

/**
 * 날씨 / 지형 / 도구 — UI 순환용 옵션과 id
 */
object BattleContext {

    data class Option(val id: String, val labelKo: String)

    val WEATHERS = listOf(
        Option("none", "없음"),
        Option("sun", "쾌청"),
        Option("rain", "비"),
        Option("sand", "모래바람"),
        Option("snow", "눈")
    )

    val TERRAINS = listOf(
        Option("none", "없음"),
        Option("electric", "일렉트릭필드"),
        Option("grassy", "그래스필드"),
        Option("psychic", "사이코필드"),
        Option("misty", "미스트필드")
    )

    val WALLS = listOf(
        Option("none", "없음"),
        Option("reflect", "리플렉터"),
        Option("light-screen", "빛의장막"),
        Option("aurora-veil", "오로라베일")
    )

    val STATUS_CONDITIONS = listOf(
        Option("none", "상태이상 없음"),
        Option("brn", "화상"),
        Option("par", "마비")
    )

    /** 공격 측 도구 (위력·공격 스탯 계열) */
    val ATTACKER_HELD = listOf(
        Option("none", "도구 없음"),
        Option("life-orb", "생명구슬"),
        Option("choice-band", "구애안경"),
        Option("choice-specs", "구애스펙터스"),
        Option("choice-scarf", "구애스카프"),
        Option("expert-belt", "고집스카프"),
        Option("muscle-band", "힘의띠"),
        Option("wise-glasses", "지혜안경"),
        Option("iron-ball", "아이언볼")
    )

    /** 방어 측 도구 */
    val DEFENDER_HELD = listOf(
        Option("none", "도구 없음"),
        Option("assault-vest", "돌격조끼"),
        Option("iron-ball", "아이언볼")
    )

    fun labelKo(options: List<Option>, id: String): String =
        options.find { it.id == id }?.labelKo ?: id

    fun nextId(options: List<Option>, currentId: String): String {
        val idx = options.indexOfFirst { it.id == currentId }.let { if (it < 0) 0 else it }
        return options[(idx + 1) % options.size].id
    }

    /**
     * 중앙 계산 보드 — 타입 일치 시 피해 1.2배 도구 (전기구슬은 피카츄 전용 2.0배)
     */
    val TYPE_BOOST_HELD = listOf(
        Option("none", "없음"),
        Option("silk-scarf", "실크스카프"),
        Option("miracle-seed", "기적의씨"),
        Option("charcoal", "목탄"),
        Option("mystic-water", "신비의물방울"),
        Option("magnet", "자석"),
        Option("silver-powder", "은빛가루"),
        Option("sharp-beak", "예리한부리"),
        Option("hard-stone", "딱딱한돌"),
        Option("poison-barb", "독바늘"),
        Option("soft-sand", "부드러운모래"),
        Option("never-melt-ice", "녹지않는얼음"),
        Option("black-belt", "검은띠"),
        Option("twisted-spoon", "휘어진스푼"),
        Option("spell-tag", "저주의부적"),
        Option("dragon-fang", "용의이빨"),
        Option("black-glasses", "검은안경"),
        Option("metal-coat", "금속코트"),
        Option("fairy-feather", "요정의깃털"),
        Option("light-ball", "전기구슬")
    )

    /** 도구 id → 강화 타입 (전기구슬은 별도 처리) */
    private val TYPE_BOOST_MAP = mapOf(
        "silk-scarf" to "normal",
        "miracle-seed" to "grass",
        "charcoal" to "fire",
        "mystic-water" to "water",
        "magnet" to "electric",
        "silver-powder" to "bug",
        "sharp-beak" to "flying",
        "hard-stone" to "rock",
        "poison-barb" to "poison",
        "soft-sand" to "ground",
        "never-melt-ice" to "ice",
        "black-belt" to "fighting",
        "twisted-spoon" to "psychic",
        "spell-tag" to "ghost",
        "dragon-fang" to "dragon",
        "black-glasses" to "dark",
        "metal-coat" to "steel",
        "fairy-feather" to "fairy"
    )

    fun typeBoostDamageMultiplier(itemId: String, moveType: String, attackerNameKo: String): Double {
        if (itemId == "none") return 1.0
        if (itemId == "light-ball") {
            return if (attackerNameKo.contains("피카츄")) 2.0 else 1.0
        }
        val boostType = TYPE_BOOST_MAP[itemId] ?: return 1.0
        return if (moveType.equals(boostType, ignoreCase = true)) 1.2 else 1.0
    }
}

package com.poketft.overlay

/**
 * 날씨 / 지형 / 도구 — UI 순환용 옵션과 id
 */
object BattleContext {

    data class Option(val id: String, val labelKo: String)

    val WEATHERS = listOf(
        Option("none", "날씨 없음"),
        Option("sun", "쾌청"),
        Option("rain", "비"),
        Option("sand", "모래바람"),
        Option("snow", "설경")
    )

    val TERRAINS = listOf(
        Option("none", "필드 없음"),
        Option("electric", "일렉트릭"),
        Option("grassy", "그래스"),
        Option("psychic", "사이킥"),
        Option("misty", "미스티")
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

    /** 벽 (리플렉터 / 빛의장막 / 오로라베일) */
    val WALLS = listOf(
        Option("none", "벽 없음"),
        Option("reflect", "리플렉터"),
        Option("light-screen", "빛의장막"),
        Option("aurora-veil", "오로라베일")
    )

    fun labelKo(options: List<Option>, id: String): String =
        options.find { it.id == id }?.labelKo ?: id

    fun nextId(options: List<Option>, currentId: String): String {
        val idx = options.indexOfFirst { it.id == currentId }.let { if (it < 0) 0 else it }
        return options[(idx + 1) % options.size].id
    }
}

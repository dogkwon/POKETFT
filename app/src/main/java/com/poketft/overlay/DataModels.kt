package com.poketft.overlay

/** 포켓몬 데이터 */
data class Pokemon(
    val id: Int,
    val dex_no: Int,
    val name_ko: String,
    val stats: List<Int>,            // [HP, Atk, Def, SpA, SpD, Spe]
    val types: List<String>,         // ["fire","flying"] 등
    val abilities: List<AbilityInfo> = emptyList(),
    val learnable_moves: List<Int>   // 기술 ID 목록
)

/** 특성 정보 */
data class AbilityInfo(
    val name_ko: String,
    val name_en: String = "",
    val is_hidden: Boolean = false
)

/** 기술 데이터 */
data class Move(
    val id: Int,
    val name_ko: String,
    val power: Int,
    val type: String,
    val category: String // "physical" or "special"
)

/** 성격 데이터 — 5×5 격자 (상승 스탯 × 하락 스탯) */
data class NatureData(
    val nameKo: String,
    val upIndex: Int,   // 1=Atk,2=Def,3=SpA,4=SpD,5=Spe, 0=none
    val downIndex: Int
) {
    val isNeutral get() = upIndex == downIndex

    fun multiplier(statIdx: Int): Double {
        if (isNeutral) return 1.0
        if (statIdx == upIndex) return 1.1
        if (statIdx == downIndex) return 0.9
        return 1.0
    }

    companion object {
        val GRID: Array<Array<NatureData>> = arrayOf(
            arrayOf(
                NatureData("노력", 1, 1),
                NatureData("외로움", 1, 2),
                NatureData("고집", 1, 3),
                NatureData("개구쟁이", 1, 4),
                NatureData("용감", 1, 5)
            ),
            arrayOf(
                NatureData("대담", 2, 1),
                NatureData("온순", 2, 2),
                NatureData("장난꾸러기", 2, 3),
                NatureData("촐랑", 2, 4),
                NatureData("무사태평", 2, 5)
            ),
            arrayOf(
                NatureData("조심", 3, 1),
                NatureData("얌전", 3, 2),
                NatureData("수줍음", 3, 3),
                NatureData("덜렁", 3, 4),
                NatureData("냉정", 3, 5)
            ),
            arrayOf(
                NatureData("차분", 4, 1),
                NatureData("온화", 4, 2),
                NatureData("신중", 4, 3),
                NatureData("변덕", 4, 4),
                NatureData("건방", 4, 5)
            ),
            arrayOf(
                NatureData("겁쟁이", 5, 1),
                NatureData("성급", 5, 2),
                NatureData("명랑", 5, 3),
                NatureData("천진난만", 5, 4),
                NatureData("성실", 5, 5)
            )
        )

        val NEUTRAL = GRID[0][0]
        val STAT_LABELS = arrayOf("공격", "방어", "특공", "특방", "스피드")
    }
}

/** 타입 한국어 매핑 */
object TypeNames {
    private val map = mapOf(
        "normal" to "노말", "fire" to "불꽃", "water" to "물", "grass" to "풀",
        "electric" to "전기", "ice" to "얼음", "fighting" to "격투", "poison" to "독",
        "ground" to "땅", "flying" to "비행", "psychic" to "에스퍼", "bug" to "벌레",
        "rock" to "바위", "ghost" to "고스트", "dragon" to "드래곤", "dark" to "악",
        "steel" to "강철", "fairy" to "페어리"
    )
    fun toKo(eng: String): String = map[eng.lowercase()] ?: eng
}

package com.poketft.overlay

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 등록된 내 포켓몬 데이터 (직렬화용)
 */
data class MyPokemonSave(
    val pokemonId: Int,
    val natureUp: Int,
    val natureDown: Int,
    val evs: List<Int>,
    val moveIds: List<Int>,
    val abilityEn: String = "",
    val abilityKo: String = "",
    val heldItemId: String = "none",       // 장착 도구
    val typeBoostHeldId: String = "none"  // 타입 부스트 도구
) {
    fun toNature(): NatureData {
        val row = natureUp - 1
        val col = natureDown - 1
        return if (row in 0..4 && col in 0..4) {
            NatureData.GRID[row][col]
        } else {
            NatureData.NEUTRAL
        }
    }
}

/**
 * 내 포켓몬 저장소 — SharedPreferences + Gson
 */
object MyPokemonStore {
    private const val PREF_NAME = "poketft_prefs"
    private const val KEY_LIST = "my_pokemon_list"
    private val gson = Gson()

    val list = mutableStateListOf<MyPokemonSave>()

    fun load(ctx: Context) {
        val prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LIST, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<MyPokemonSave>>() {}.type
                val saved: List<MyPokemonSave> = gson.fromJson(json, type)
                list.clear()
                // Gson은 Kotlin default value를 무시하고 누락 필드를 null로 채움
                // → 새로 추가된 String 필드가 null일 수 있으므로 null-safe 변환
                list.addAll(saved.map { s ->
                    s.copy(
                        abilityEn        = s.abilityEn        ?: "",
                        abilityKo        = s.abilityKo        ?: "",
                        heldItemId       = s.heldItemId       ?: "none",
                        typeBoostHeldId  = s.typeBoostHeldId  ?: "none"
                    )
                })
            } catch (_: Exception) {}
        }
    }

    fun save(ctx: Context) {
        val prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LIST, gson.toJson(list.toList())).apply()
    }

    fun add(ctx: Context, pokemon: MyPokemonSave) {
        list.add(pokemon)
        save(ctx)
    }

    fun remove(ctx: Context, index: Int) {
        if (index in list.indices) {
            list.removeAt(index)
            save(ctx)
        }
    }

    fun update(ctx: Context, index: Int, pokemon: MyPokemonSave) {
        if (index in list.indices) {
            list[index] = pokemon
            save(ctx)
        }
    }

    /** 등록된 포켓몬의 실제 Pokemon 객체를 반환 */
    fun getBasePokemon(save: MyPokemonSave): Pokemon? {
        return Repo.pokemons.find { it.id == save.pokemonId }
    }
}

package com.poketft.overlay

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

/**
 * assets/ 폴더의 JSON 데이터를 로딩하고 검색하는 리포지토리
 */
object Repo {
    var pokemons: List<Pokemon> = emptyList()
    var movesById: Map<Int, Move> = emptyMap()

    fun load(ctx: Context) {
        val gson = Gson()
        try {
            val pokemonReader = InputStreamReader(ctx.assets.open("pokemon_db.json"))
            val pokemonType = object : TypeToken<List<Pokemon>>() {}.type
            pokemons = gson.fromJson(pokemonReader, pokemonType)
            pokemonReader.close()
        } catch (_: Exception) {
            pokemons = emptyList()
        }
        try {
            val moveReader = InputStreamReader(ctx.assets.open("moves_db.json"))
            val moveType = object : TypeToken<List<Move>>() {}.type
            val moveList: List<Move> = gson.fromJson(moveReader, moveType)
            movesById = moveList.associateBy { it.id }
            moveReader.close()
        } catch (_: Exception) {
            movesById = emptyMap()
        }
    }

    /** 이름 또는 도감번호로 포켓몬 검색 */
    fun search(query: String): List<Pokemon> {
        if (query.isBlank()) return pokemons
        val q = query.trim().lowercase()
        return pokemons.filter {
            it.name_ko.lowercase().contains(q) || it.dex_no.toString().contains(q)
        }
    }

    /** 포켓몬이 배울 수 있는 기술 목록 반환 (위력 > 0인 것만) */
    fun getLearnableMoves(pokemon: Pokemon): List<Move> {
        return pokemon.learnable_moves.mapNotNull { movesById[it] }
            .filter { it.power > 0 }
            .sortedByDescending { it.power }
    }
}

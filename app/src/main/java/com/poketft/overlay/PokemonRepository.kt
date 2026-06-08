package com.poketft.overlay

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

/**
 * assets/pokemon_db.json, moves_db.json 로딩 및 검색
 */
object Repo {
    private const val TAG = "PoketftRepo"

    var pokemons: List<Pokemon> = emptyList()
        private set
    var movesById: Map<Int, Move> = emptyMap()
        private set

    val pokemonCount: Int get() = pokemons.size
    val moveCount: Int get() = movesById.size

    fun load(ctx: Context) {
        val gson = Gson()
        pokemons = loadPokemonList(ctx, gson)
        movesById = loadMovesMap(ctx, gson)
        Log.i(TAG, "loaded pokemon=${pokemons.size} moves=${movesById.size}")
    }

    /** 오버레이 등에서 DB가 비었을 때 재로드 */
    fun ensureLoaded(ctx: Context): Boolean {
        if (pokemons.isNotEmpty() && movesById.isNotEmpty()) return true
        load(ctx)
        return pokemons.isNotEmpty()
    }

    private fun loadPokemonList(ctx: Context, gson: Gson): List<Pokemon> {
        return try {
            ctx.assets.open("pokemon_db.json").use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                    val type = object : TypeToken<List<Pokemon>>() {}.type
                    val list: List<Pokemon>? = gson.fromJson(reader, type)
                    list?.filter { it.name_ko.isNotBlank() && it.stats.size >= 6 } ?: emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "pokemon_db.json load failed", e)
            emptyList()
        }
    }

    private fun loadMovesMap(ctx: Context, gson: Gson): Map<Int, Move> {
        return try {
            ctx.assets.open("moves_db.json").use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                    val type = object : TypeToken<List<Move>>() {}.type
                    val moveList: List<Move>? = gson.fromJson(reader, type)
                    moveList?.associateBy { it.id } ?: emptyMap()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "moves_db.json load failed", e)
            emptyMap()
        }
    }

    fun search(query: String): List<Pokemon> = filterByNameKo(query, limit = 50)

    /** name_ko 실시간 필터 */
    fun filterByNameKo(query: String, limit: Int = 10): List<Pokemon> {
        val q = query.trim()
        if (q.isEmpty() || pokemons.isEmpty()) return emptyList()
        return pokemons
            .filter { p ->
                p.name_ko.contains(q, ignoreCase = true) ||
                    p.dex_no.toString().contains(q) ||
                    p.name_ko.replace(" ", "").contains(q.replace(" ", ""), ignoreCase = true)
            }
            .sortedWith(
                compareBy<Pokemon> {
                    when {
                        it.name_ko.equals(q, ignoreCase = true) -> 0
                        it.name_ko.startsWith(q, ignoreCase = true) -> 1
                        else -> 2
                    }
                }.thenBy { it.name_ko.length }
            )
            .take(limit)
    }

    fun getLearnableMoves(pokemon: Pokemon): List<Move> {
        return pokemon.learnable_moves.mapNotNull { movesById[it] }
            .filter { it.power > 0 }
            .sortedByDescending { it.power }
    }

    /** moves_db.json 전체에서 name_ko로 검색 (포켓몬 무관) */
    fun filterMovesByName(query: String, limit: Int = 30): List<Move> {
        val q = query.trim()
        if (q.isEmpty()) return movesById.values
            .filter { it.power > 0 }
            .sortedBy { it.name_ko }
            .take(limit)
        return movesById.values
            .filter { it.power > 0 && it.name_ko.contains(q, ignoreCase = true) }
            .sortedWith(
                compareBy<Move> {
                    when {
                        it.name_ko.equals(q, ignoreCase = true) -> 0
                        it.name_ko.startsWith(q, ignoreCase = true) -> 1
                        else -> 2
                    }
                }.thenBy { it.name_ko }
            )
            .take(limit)
    }

    fun findById(id: Int): Pokemon? = pokemons.find { it.id == id }
}

package com.khangtm.pokepoke.data.pokemon.api

import com.khangtm.pokepoke.core.common.DataResult
import kotlinx.coroutines.flow.Flow

/**
 * Shared repository (single source of truth). Every function returns a [DataResult] and never throws.
 * "No data" when observing a single record is `Failure(DataError.NotFound)`.
 */
interface PokemonRepository {
    /** Pokémon that have a list position, ordered by position, at most [limit] (empty when nothing is stored yet). */
    fun observeList(limit: Int): Flow<DataResult<List<PokemonSummary>>>

    /** Null = no page has ever been saved. */
    fun observeTotalCount(): Flow<DataResult<Int?>>

    /** Upserts each item (name, imageUrl) and assigns the list position `offset + index`. Never touches detail data. */
    suspend fun saveList(items: List<PokemonSummary>, offset: Int, totalCount: Int): DataResult<Unit>

    /** Upserts name / imageUrl; keeps the list position; never touches detail data. */
    suspend fun saveSummary(summary: PokemonSummary): DataResult<Unit>

    fun observeDetail(id: Int): Flow<DataResult<PokemonDetail>>

    /** `saveSummary(detail.summary)` + upsert of the detail in one transaction. Keeps the list position. */
    suspend fun saveDetail(detail: PokemonDetail): DataResult<Unit>

    /** [query] is normalized (trim, lowercase). All digits → matched by id; anything else → matched by name. */
    fun observeByNameOrId(query: String): Flow<DataResult<PokemonSummary>>

    /** Keeps the order of [ids]; ids missing from storage are skipped. */
    fun observeSummaries(ids: List<Int>): Flow<DataResult<List<PokemonSummary>>>
}

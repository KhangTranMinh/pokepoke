package com.khangtm.pokepoke.business.domain.pokemon

import com.khangtm.pokepoke.core.common.DataResult
import kotlinx.coroutines.flow.Flow

/**
 * Pokémon repository (single source of truth). `observe*` only reads local data, `refresh*` calls the network and
 * then saves (one-shot, layers.md §1.2). Every function returns a [DataResult] and never throws.
 * "No data" when observing a single record is `Failure(DataError.NotFound)`.
 * There is no public `save*`: how data is stored (merge rules) is the job of `:business:data`.
 */
interface PokemonRepository {
    /** Pokémon that have a list position, ordered by position, at most [limit] (empty when nothing is stored yet). */
    fun observeList(limit: Int): Flow<DataResult<List<PokemonSummary>>>

    /** Null = no page has ever been saved. */
    fun observeTotalCount(): Flow<DataResult<Int?>>

    fun observeDetail(id: Int): Flow<DataResult<PokemonDetail>>

    /** [query] is normalized (trim, lowercase). All digits → matched by id; anything else → matched by name. */
    fun observeByNameOrId(query: String): Flow<DataResult<PokemonSummary>>

    /** Keeps the order of [ids]; ids missing from storage are skipped. */
    fun observeSummaries(ids: List<Int>): Flow<DataResult<List<PokemonSummary>>>

    /**
     * Fetches page `[offset, offset + limit)` from the API and saves it (list position `offset + index`, updates the
     * total count, never touches detail).
     */
    suspend fun refreshPage(offset: Int, limit: Int): DataResult<Unit>

    /**
     * [query] is normalized. Fetches one Pokémon by name / id and saves its summary (keeps the list position, never
     * touches detail). 404 → `Failure(NotFound)`.
     */
    suspend fun refreshByNameOrId(query: String): DataResult<Unit>

    /** Fetches the detail (pokemon → species → evolution-chain) and saves summary + detail in one transaction. */
    suspend fun refreshDetail(id: Int): DataResult<Unit>
}

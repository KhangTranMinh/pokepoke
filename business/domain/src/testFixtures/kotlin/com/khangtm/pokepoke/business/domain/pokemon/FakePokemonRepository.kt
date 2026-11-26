package com.khangtm.pokepoke.business.domain.pokemon

import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update

/**
 * In-memory [PokemonRepository] with the same merge rules as the Room implementation in `:business:data`.
 *
 * - Fake server: set [serverList] / [serverDetails]; `refresh*` copy from it into the fake storage
 *   (not on the server → `Failure(NotFound)`).
 * - [refreshError] (not null) makes every `refresh*` return that failure and leaves the storage unchanged.
 * - [failWith] (not null) makes every function return that failure.
 * - `seed*` write the storage directly; [refreshCalls] counts `refresh*` calls (including failed ones).
 */
class FakePokemonRepository : PokemonRepository {

    private data class State(
        val summaries: Map<Int, PokemonSummary> = emptyMap(),
        val positions: Map<Int, Int> = emptyMap(),
        /** Detail without its summary (the summary is looked up when reading, like the SQL join). */
        val details: Map<Int, PokemonDetail> = emptyMap(),
        val totalCount: Int? = null,
    )

    private val state = MutableStateFlow(State())
    private val failure = MutableStateFlow<DataError?>(null)

    /** Pokémon of the fake server, in API list order. */
    var serverList: List<PokemonSummary> = emptyList()

    /** Details of the fake server, by id. */
    var serverDetails: Map<Int, PokemonDetail> = emptyMap()

    /** Not null: every `refresh*` returns this failure and the storage does not change. */
    var refreshError: DataError? = null

    var failWith: DataError?
        get() = failure.value
        set(value) {
            failure.value = value
        }

    /** Number of `refresh*` calls so far. */
    var refreshCalls: Int = 0
        private set

    fun seedList(items: List<PokemonSummary>, offset: Int, totalCount: Int) = state.update { s -> s.withList(items, offset, totalCount) }

    fun seedSummary(summary: PokemonSummary) = state.update { it.withSummary(summary) }

    fun seedDetail(detail: PokemonDetail) = state.update { it.withDetail(detail) }

    private fun State.withList(items: List<PokemonSummary>, offset: Int, totalCount: Int): State {
        var summaries = summaries
        var positions = positions
        items.forEachIndexed { index, item ->
            val position = offset + index
            summaries = summaries + (item.id to item)
            positions = positions.filterValues { it != position } + (item.id to position)
        }
        return copy(summaries = summaries, positions = positions, totalCount = totalCount)
    }

    private fun State.withSummary(summary: PokemonSummary): State = copy(summaries = summaries + (summary.id to summary))

    private fun State.withDetail(detail: PokemonDetail): State = copy(
        summaries = summaries + (detail.summary.id to detail.summary),
        details = details + (detail.summary.id to detail),
    )

    private fun <T> observe(read: (State) -> DataResult<T>): Flow<DataResult<T>> =
        combine(state, failure) { current, error ->
            if (error != null) DataResult.Failure(error) else read(current)
        }.distinctUntilChanged()

    private inline fun refresh(block: () -> DataResult<State>): DataResult<Unit> {
        refreshCalls++
        failWith?.let { return DataResult.Failure(it) }
        refreshError?.let { return DataResult.Failure(it) }
        return when (val result = block()) {
            is DataResult.Success -> {
                state.value = result.value
                DataResult.Success(Unit)
            }
            is DataResult.Failure -> result
        }
    }

    private fun <T : Any> T?.orNotFound(): DataResult<T> =
        if (this == null) DataResult.Failure(DataError.NotFound) else DataResult.Success(this)

    override fun observeList(limit: Int): Flow<DataResult<List<PokemonSummary>>> = observe { s ->
        DataResult.Success(
            s.positions.entries.sortedBy { it.value }.take(limit).mapNotNull { s.summaries[it.key] },
        )
    }

    override fun observeTotalCount(): Flow<DataResult<Int?>> = observe { DataResult.Success(it.totalCount) }

    override fun observeDetail(id: Int): Flow<DataResult<PokemonDetail>> = observe { s ->
        val summary = s.summaries[id]
        s.details[id]?.let { detail -> summary?.let { detail.copy(summary = it) } }.orNotFound()
    }

    override fun observeByNameOrId(query: String): Flow<DataResult<PokemonSummary>> {
        val normalized = query.trim().lowercase()
        val isId = normalized.isNotEmpty() && normalized.all { it in '0'..'9' }
        return observe { s ->
            if (isId) {
                normalized.toIntOrNull()?.let { s.summaries[it] }.orNotFound()
            } else {
                s.summaries.values.firstOrNull { it.name == normalized }.orNotFound()
            }
        }
    }

    override fun observeSummaries(ids: List<Int>): Flow<DataResult<List<PokemonSummary>>> = observe { s ->
        DataResult.Success(ids.mapNotNull { s.summaries[it] })
    }

    override suspend fun refreshPage(offset: Int, limit: Int): DataResult<Unit> = refresh {
        val items = serverList.drop(offset).take(limit)
        DataResult.Success(state.value.withList(items, offset, serverList.size))
    }

    override suspend fun refreshByNameOrId(query: String): DataResult<Unit> = refresh {
        val normalized = query.trim().lowercase()
        val isId = normalized.isNotEmpty() && normalized.all { it in '0'..'9' }
        val known = serverList + serverDetails.values.map { it.summary }
        val found = if (isId) {
            normalized.toIntOrNull()?.let { id -> known.firstOrNull { it.id == id } }
        } else {
            known.firstOrNull { it.name == normalized }
        }
        found?.let { DataResult.Success(state.value.withSummary(it)) } ?: DataResult.Failure(DataError.NotFound)
    }

    override suspend fun refreshDetail(id: Int): DataResult<Unit> = refresh {
        serverDetails[id]?.let { DataResult.Success(state.value.withDetail(it)) } ?: DataResult.Failure(DataError.NotFound)
    }
}

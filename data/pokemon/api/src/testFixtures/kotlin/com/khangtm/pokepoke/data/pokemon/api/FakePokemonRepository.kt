package com.khangtm.pokepoke.data.pokemon.api

import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update

/**
 * In-memory [PokemonRepository] with the same merge rules as the Room implementation.
 * Set [failWith] to make every function return `Failure` (to test error branches).
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

    var failWith: DataError?
        get() = failure.value
        set(value) {
            failure.value = value
        }

    private fun <T> observe(read: (State) -> DataResult<T>): Flow<DataResult<T>> =
        combine(state, failure) { current, error ->
            if (error != null) DataResult.Failure(error) else read(current)
        }.distinctUntilChanged()

    private inline fun write(block: () -> Unit): DataResult<Unit> {
        val error = failWith
        if (error != null) return DataResult.Failure(error)
        block()
        return DataResult.Success(Unit)
    }

    private fun <T : Any> T?.orNotFound(): DataResult<T> =
        if (this == null) DataResult.Failure(DataError.NotFound) else DataResult.Success(this)

    override fun observeList(limit: Int): Flow<DataResult<List<PokemonSummary>>> = observe { s ->
        DataResult.Success(
            s.positions.entries.sortedBy { it.value }.take(limit).mapNotNull { s.summaries[it.key] },
        )
    }

    override fun observeTotalCount(): Flow<DataResult<Int?>> = observe { DataResult.Success(it.totalCount) }

    override suspend fun saveList(items: List<PokemonSummary>, offset: Int, totalCount: Int): DataResult<Unit> = write {
        state.update { s ->
            var summaries = s.summaries
            var positions = s.positions
            items.forEachIndexed { index, item ->
                val position = offset + index
                summaries = summaries + (item.id to item)
                positions = positions.filterValues { it != position } + (item.id to position)
            }
            s.copy(summaries = summaries, positions = positions, totalCount = totalCount)
        }
    }

    override suspend fun saveSummary(summary: PokemonSummary): DataResult<Unit> = write {
        state.update { it.copy(summaries = it.summaries + (summary.id to summary)) }
    }

    override fun observeDetail(id: Int): Flow<DataResult<PokemonDetail>> = observe { s ->
        val summary = s.summaries[id]
        s.details[id]?.let { detail -> summary?.let { detail.copy(summary = it) } }.orNotFound()
    }

    override suspend fun saveDetail(detail: PokemonDetail): DataResult<Unit> = write {
        state.update {
            it.copy(
                summaries = it.summaries + (detail.summary.id to detail.summary),
                details = it.details + (detail.summary.id to detail),
            )
        }
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
}

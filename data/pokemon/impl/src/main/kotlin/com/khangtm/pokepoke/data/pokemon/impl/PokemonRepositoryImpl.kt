package com.khangtm.pokepoke.data.pokemon.impl

import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import com.khangtm.pokepoke.core.common.IoDispatcher
import com.khangtm.pokepoke.core.common.asDataResult
import com.khangtm.pokepoke.core.common.dataResultOf
import com.khangtm.pokepoke.core.common.flatMap
import com.khangtm.pokepoke.data.pokemon.api.PokemonDetail
import com.khangtm.pokepoke.data.pokemon.api.PokemonRepository
import com.khangtm.pokepoke.data.pokemon.api.PokemonSummary
import com.khangtm.pokepoke.data.pokemon.impl.db.PokemonDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Room-backed [PokemonRepository]. The DAO is the data source; this class is the only place that catches exceptions. */
internal class PokemonRepositoryImpl @Inject constructor(
    private val dao: PokemonDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PokemonRepository {

    private val codec = PokemonJsonCodec()

    /** Creating a Room flow can already throw (closed database), so it is created inside the collected flow. */
    private fun <T> readFlow(source: () -> Flow<T>): Flow<T> = flow { emitAll(source()) }

    private fun <T> Flow<T>.asLocalRead(): Flow<DataResult<T>> =
        flowOn(ioDispatcher).asDataResult { DataError.Local.Read(it) }

    private fun <T : Any> Flow<DataResult<T?>>.orNotFound(): Flow<DataResult<T>> = map { result ->
        result.flatMap { value ->
            if (value == null) DataResult.Failure(DataError.NotFound) else DataResult.Success(value)
        }
    }

    private suspend inline fun <T> write(crossinline block: suspend () -> T): DataResult<T> =
        dataResultOf({ DataError.Local.Write(it) }) { withContext(ioDispatcher) { block() } }

    override fun observeList(limit: Int): Flow<DataResult<List<PokemonSummary>>> =
        readFlow { dao.observeList(limit) }.map { list -> list.map { it.toModel() } }.asLocalRead()

    override fun observeTotalCount(): Flow<DataResult<Int?>> = readFlow { dao.observeTotalCount() }.asLocalRead()

    override suspend fun saveList(items: List<PokemonSummary>, offset: Int, totalCount: Int): DataResult<Unit> =
        write { dao.saveList(items.map { it.toEntity() }, offset, totalCount) }

    override suspend fun saveSummary(summary: PokemonSummary): DataResult<Unit> =
        write { dao.saveSummary(summary.id, summary.name, summary.imageUrl) }

    override fun observeDetail(id: Int): Flow<DataResult<PokemonDetail>> =
        readFlow { dao.observeDetail(id) }.map { row -> row?.toModel(codec) }.asLocalRead().orNotFound()

    override suspend fun saveDetail(detail: PokemonDetail): DataResult<Unit> =
        write { dao.saveDetail(detail.summary.toEntity(), detail.toEntity(codec, System.currentTimeMillis())) }

    override fun observeByNameOrId(query: String): Flow<DataResult<PokemonSummary>> {
        val normalized = query.trim().lowercase()
        val isId = normalized.isNotEmpty() && normalized.all { it in '0'..'9' }
        return readFlow {
            if (isId) {
                // An id too large for Int cannot exist in storage.
                normalized.toIntOrNull()?.let { dao.observeById(it) } ?: flowOf(null)
            } else {
                dao.observeByName(normalized)
            }
        }.map { it?.toModel() }.asLocalRead().orNotFound()
    }

    override fun observeSummaries(ids: List<Int>): Flow<DataResult<List<PokemonSummary>>> =
        readFlow { dao.observeByIds(ids) }.map { entities ->
            val byId = entities.associateBy { it.id }
            ids.mapNotNull { byId[it]?.toModel() }
        }.asLocalRead()
}

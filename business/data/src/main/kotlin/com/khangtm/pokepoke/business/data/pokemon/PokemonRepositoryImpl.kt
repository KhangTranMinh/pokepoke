package com.khangtm.pokepoke.business.data.pokemon

import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import com.khangtm.pokepoke.core.common.IoDispatcher
import com.khangtm.pokepoke.core.common.asDataResult
import com.khangtm.pokepoke.core.common.dataResultOf
import com.khangtm.pokepoke.core.common.flatMap
import com.khangtm.pokepoke.core.connectivity.NetworkMonitor
import com.khangtm.pokepoke.core.network.toNetworkDataError
import com.khangtm.pokepoke.business.domain.pokemon.PokemonDetail
import com.khangtm.pokepoke.business.domain.pokemon.PokemonRepository
import com.khangtm.pokepoke.business.domain.pokemon.PokemonSummary
import com.khangtm.pokepoke.business.data.pokemon.db.PokemonDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [PokemonRepository] backed by PokeAPI (Retrofit) and Room. The API and the DAO are the data sources; this class
 * is the only place that catches exceptions.
 */
internal class PokemonRepositoryImpl @Inject constructor(
    private val api: PokemonApi,
    private val dao: PokemonDao,
    private val networkMonitor: NetworkMonitor,
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

    /**
     * network → map → save. Offline per [NetworkMonitor] → `Network.NoConnection` at once, without calling the API.
     * A network or map error leaves the storage unchanged; a write error is `Local.Write`.
     */
    private suspend inline fun <T> refresh(
        crossinline fetch: suspend () -> T,
        crossinline save: suspend (T) -> Unit,
    ): DataResult<Unit> {
        if (!networkMonitor.isOnlineNow()) return DataResult.Failure(DataError.Network.NoConnection)
        return dataResultOf(Throwable::toNetworkDataError) { withContext(ioDispatcher) { fetch() } }
            .flatMap { value -> write { save(value) } }
    }

    override fun observeList(limit: Int): Flow<DataResult<List<PokemonSummary>>> =
        readFlow { dao.observeList(limit) }.map { list -> list.map { it.toModel() } }.asLocalRead()

    override fun observeTotalCount(): Flow<DataResult<Int?>> = readFlow { dao.observeTotalCount() }.asLocalRead()

    override fun observeDetail(id: Int): Flow<DataResult<PokemonDetail>> =
        readFlow { dao.observeDetail(id) }.map { row -> row?.toModel(codec) }.asLocalRead().orNotFound()

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

    override suspend fun refreshPage(offset: Int, limit: Int): DataResult<Unit> = refresh(
        fetch = { api.list(offset, limit) },
        save = { page -> dao.saveList(page.results.mapNotNull { it.toSummary() }.map { it.toEntity() }, offset, page.count) },
    )

    override suspend fun refreshByNameOrId(query: String): DataResult<Unit> = refresh(
        fetch = { api.pokemonBasic(query).toSummary() },
        save = { summary -> dao.saveSummary(summary.id, summary.name, summary.imageUrl) },
    )

    override suspend fun refreshDetail(id: Int): DataResult<Unit> = refresh(
        fetch = {
            // pokemon → species → evolution-chain: the species / chain ids only exist to reach the chain.
            val pokemon = api.pokemonDetail(id)
            val species = api.species(pokemon.speciesId())
            pokemon.toDetail(api.evolutionChain(species.evolutionChainId()))
        },
        save = { detail -> dao.saveDetail(detail.summary.toEntity(), detail.toEntity(codec, System.currentTimeMillis())) },
    )
}

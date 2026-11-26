package com.khangtm.pokepoke.business.data.favourite

import com.khangtm.pokepoke.business.data.favourite.db.FavouriteDao
import com.khangtm.pokepoke.business.domain.favourite.FavouriteEntry
import com.khangtm.pokepoke.business.domain.favourite.FavouriteRepository
import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import com.khangtm.pokepoke.core.common.IoDispatcher
import com.khangtm.pokepoke.core.common.asDataResult
import com.khangtm.pokepoke.core.common.dataResultOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** [FavouriteRepository] backed by Room. The DAO is the data source; this class is the only place that catches exceptions. */
internal class FavouriteRepositoryImpl @Inject constructor(
    private val dao: FavouriteDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FavouriteRepository {

    /** Creating a Room flow can already throw (closed database), so it is created inside the collected flow. */
    private fun <T> readFlow(source: () -> Flow<T>): Flow<DataResult<T>> =
        flow { emitAll(source()) }.flowOn(ioDispatcher).asDataResult { DataError.Local.Read(it) }

    private suspend inline fun <T> write(crossinline block: suspend () -> T): DataResult<T> =
        dataResultOf({ DataError.Local.Write(it) }) { withContext(ioDispatcher) { block() } }

    override fun observeFavourites(): Flow<DataResult<List<FavouriteEntry>>> =
        readFlow { dao.observeAll().map { list -> list.map { it.toModel() } } }

    override fun observeIsFavourite(pokemonId: Int): Flow<DataResult<Boolean>> =
        readFlow { dao.observeExists(pokemonId) }

    override suspend fun toggle(pokemonId: Int): DataResult<Boolean> =
        write { dao.toggle(pokemonId, System.currentTimeMillis()) }

    override suspend fun remove(pokemonId: Int): DataResult<Unit> = write { dao.remove(pokemonId) }

    override suspend fun clear(): DataResult<Unit> = write { dao.clear() }
}

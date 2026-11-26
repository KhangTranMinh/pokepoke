package com.khangtm.pokepoke.business.domain.favourite

import com.khangtm.pokepoke.core.common.DataResult
import kotlinx.coroutines.flow.Flow

/** Implemented in `:business:data`. Every function returns a [DataResult]. */
interface FavouriteRepository {
    /** Newest first. */
    fun observeFavourites(): Flow<DataResult<List<FavouriteEntry>>>

    fun observeIsFavourite(pokemonId: Int): Flow<DataResult<Boolean>>

    /** In one transaction; returns the new state (`true` = just added). */
    suspend fun toggle(pokemonId: Int): DataResult<Boolean>

    suspend fun remove(pokemonId: Int): DataResult<Unit>

    suspend fun clear(): DataResult<Unit>
}

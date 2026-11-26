package com.khangtm.pokepoke.business.domain.favourite

import com.khangtm.pokepoke.core.common.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * `observeFavourites()` flatMapLatest `PokemonRepository.observeSummaries(ids)`: joins `addedAt`, newest first, skips
 * ids without a summary. A failure at any step is a failure.
 */
fun interface ObserveFavouritePokemonUseCase {
    operator fun invoke(): Flow<AppResult<List<FavouritePokemon>>>
}

fun interface ObserveIsFavouriteUseCase {
    operator fun invoke(pokemonId: Int): Flow<AppResult<Boolean>>
}

/** `Success(state AFTER the toggle)`: true = just added. */
fun interface ToggleFavouriteUseCase {
    suspend operator fun invoke(pokemonId: Int): AppResult<Boolean>
}

fun interface RemoveFavouriteUseCase {
    suspend operator fun invoke(pokemonId: Int): AppResult<Unit>
}

fun interface ClearFavouritesUseCase {
    suspend operator fun invoke(): AppResult<Unit>
}

package com.khangtm.pokepoke.business.domain.favourite

import com.khangtm.pokepoke.business.domain.pokemon.PokemonRepository
import com.khangtm.pokepoke.core.common.AppResult
import com.khangtm.pokepoke.core.common.DataResult
import com.khangtm.pokepoke.core.common.toAppError
import com.khangtm.pokepoke.core.common.toAppResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class ObserveFavouritePokemonUseCaseImpl @Inject constructor(
    private val favourites: FavouriteRepository,
    private val pokemon: PokemonRepository,
) : ObserveFavouritePokemonUseCase {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(): Flow<AppResult<List<FavouritePokemon>>> =
        favourites.observeFavourites().flatMapLatest { entries ->
            when (entries) {
                is DataResult.Failure -> flowOf(AppResult.Failure(entries.error.toAppError()))
                is DataResult.Success -> {
                    val addedAt = entries.value.associate { it.pokemonId to it.addedAtEpochMillis }
                    pokemon.observeSummaries(entries.value.map { it.pokemonId }).toAppResult().map { result ->
                        when (result) {
                            is AppResult.Failure -> result
                            is AppResult.Success -> AppResult.Success(
                                result.value.mapNotNull { summary ->
                                    addedAt[summary.id]?.let { FavouritePokemon(summary, it) }
                                },
                            )
                        }
                    }
                }
            }
        }
}

internal class ObserveIsFavouriteUseCaseImpl @Inject constructor(
    private val repository: FavouriteRepository,
) : ObserveIsFavouriteUseCase {
    override fun invoke(pokemonId: Int): Flow<AppResult<Boolean>> =
        repository.observeIsFavourite(pokemonId).toAppResult()
}

internal class ToggleFavouriteUseCaseImpl @Inject constructor(
    private val repository: FavouriteRepository,
) : ToggleFavouriteUseCase {
    override suspend fun invoke(pokemonId: Int): AppResult<Boolean> = repository.toggle(pokemonId).toAppResult()
}

internal class RemoveFavouriteUseCaseImpl @Inject constructor(
    private val repository: FavouriteRepository,
) : RemoveFavouriteUseCase {
    override suspend fun invoke(pokemonId: Int): AppResult<Unit> = repository.remove(pokemonId).toAppResult()
}

internal class ClearFavouritesUseCaseImpl @Inject constructor(
    private val repository: FavouriteRepository,
) : ClearFavouritesUseCase {
    override suspend fun invoke(): AppResult<Unit> = repository.clear().toAppResult()
}

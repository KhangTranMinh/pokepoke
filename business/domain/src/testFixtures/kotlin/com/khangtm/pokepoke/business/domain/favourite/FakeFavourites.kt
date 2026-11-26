package com.khangtm.pokepoke.business.domain.favourite

import com.khangtm.pokepoke.business.domain.pokemon.PokemonSummary
import com.khangtm.pokepoke.core.common.AppError
import com.khangtm.pokepoke.core.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * The five favourite use cases over one state, for ViewModel / feature tests. A class cannot implement the five
 * `fun interface`s directly (their `invoke` signatures clash on the JVM), so each one is a property.
 *
 * - [ids]: favourite ids, newest first.
 * - [summaries]: Pokémon the observe use case can show; ids without a summary are skipped.
 * - [failWith] (not null): every use case returns this failure (read when the flow is collected / the call runs).
 *
 * The state has no `added_at`: `addedAtEpochMillis` in [observeFavouritePokemon] is a synthetic value that decreases
 * with the position (newest = highest), so the order is still visible.
 */
class FakeFavourites {

    val ids = MutableStateFlow<List<Int>>(emptyList())
    val summaries: MutableMap<Int, PokemonSummary> = mutableMapOf()
    var failWith: AppError? = null

    private fun failure(): AppResult.Failure? = failWith?.let { AppResult.Failure(it) }

    private fun <T> Flow<List<Int>>.mapResult(transform: (List<Int>) -> T): Flow<AppResult<T>> =
        map { list -> failure() ?: AppResult.Success(transform(list)) }

    val observeFavouritePokemon: ObserveFavouritePokemonUseCase = ObserveFavouritePokemonUseCase {
        ids.mapResult { list ->
            list.mapIndexedNotNull { index, id ->
                summaries[id]?.let { FavouritePokemon(it, (list.size - index).toLong()) }
            }
        }
    }

    val observeIsFavourite: ObserveIsFavouriteUseCase = ObserveIsFavouriteUseCase { id ->
        ids.mapResult { id in it }
    }

    val toggle: ToggleFavouriteUseCase = ToggleFavouriteUseCase { id ->
        failure() ?: run {
            val added = id !in ids.value
            ids.value = if (added) listOf(id) + ids.value else ids.value - id
            AppResult.Success(added)
        }
    }

    val remove: RemoveFavouriteUseCase = RemoveFavouriteUseCase { id ->
        failure() ?: run {
            ids.value = ids.value - id
            AppResult.Success(Unit)
        }
    }

    val clear: ClearFavouritesUseCase = ClearFavouritesUseCase {
        failure() ?: run {
            ids.value = emptyList()
            AppResult.Success(Unit)
        }
    }
}

package com.khangtm.pokepoke.business.domain.pokemon

import com.khangtm.pokepoke.core.common.AppResult
import com.khangtm.pokepoke.core.common.DataSnapshot
import kotlinx.coroutines.flow.Flow

const val POKEMON_PAGE_SIZE = 20

/** [visibleCount] is how many items from the start of the list can be shown now. */
data class PageResult(val visibleCount: Int, val isOffline: Boolean, val endReached: Boolean)

sealed interface SearchOutcome {
    data class Found(val pokemon: PokemonSummary, val isOffline: Boolean) : SearchOutcome
    data object NotFound : SearchOutcome
}

fun interface ObservePokemonListUseCase {
    operator fun invoke(limit: Int): Flow<AppResult<List<PokemonSummary>>>
}

fun interface LoadPokemonPageUseCase {
    suspend operator fun invoke(offset: Int): AppResult<PageResult>
}

fun interface SearchPokemonUseCase {
    suspend operator fun invoke(query: String): AppResult<SearchOutcome>
}

fun interface ObservePokemonDetailUseCase {
    operator fun invoke(id: Int): Flow<AppResult<DataSnapshot<PokemonDetail>>>
}

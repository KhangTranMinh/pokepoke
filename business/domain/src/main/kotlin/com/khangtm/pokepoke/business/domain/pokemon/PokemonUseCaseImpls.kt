package com.khangtm.pokepoke.business.domain.pokemon

import com.khangtm.pokepoke.core.common.AppError
import com.khangtm.pokepoke.core.common.AppResult
import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import com.khangtm.pokepoke.core.common.DataSnapshot
import com.khangtm.pokepoke.core.common.FetchOutcome
import com.khangtm.pokepoke.core.common.networkFirst
import com.khangtm.pokepoke.core.common.runNetworkFirst
import com.khangtm.pokepoke.core.common.toAppError
import com.khangtm.pokepoke.core.common.toAppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// The offline check lives in `refresh*` of the data layer (it returns Network.NoConnection immediately), so the
// helpers are called with `isOnline = { true }`.

internal class ObservePokemonListUseCaseImpl @Inject constructor(
    private val repository: PokemonRepository,
) : ObservePokemonListUseCase {
    override fun invoke(limit: Int): Flow<AppResult<List<PokemonSummary>>> =
        repository.observeList(limit).toAppResult()
}

internal class LoadPokemonPageUseCaseImpl @Inject constructor(
    private val repository: PokemonRepository,
) : LoadPokemonPageUseCase {
    override suspend fun invoke(offset: Int): AppResult<PageResult> {
        val outcome = runNetworkFirst({ true }) { repository.refreshPage(offset, POKEMON_PAGE_SIZE) }
        if (outcome is FetchOutcome.Failed) return AppResult.Failure(outcome.error.toAppError())
        val target = offset + POKEMON_PAGE_SIZE
        val endReached = when (val total = repository.observeTotalCount().first()) {
            is DataResult.Success -> total.value.let { it != null && target >= it }
            is DataResult.Failure -> return AppResult.Failure(total.error.toAppError())
        }
        return when (outcome) {
            FetchOutcome.Fetched -> AppResult.Success(PageResult(target, isOffline = false, endReached = endReached))
            FetchOutcome.Offline -> when (val cached = repository.observeList(target).first()) {
                is DataResult.Success ->
                    if (cached.value.size > offset) {
                        AppResult.Success(PageResult(target, isOffline = true, endReached = endReached))
                    } else {
                        AppResult.Failure(AppError.NoConnection)
                    }
                is DataResult.Failure -> AppResult.Failure(cached.error.toAppError())
            }
            is FetchOutcome.Failed -> AppResult.Failure(outcome.error.toAppError())
        }
    }
}

internal class SearchPokemonUseCaseImpl @Inject constructor(
    private val repository: PokemonRepository,
) : SearchPokemonUseCase {
    override suspend fun invoke(query: String): AppResult<SearchOutcome> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return AppResult.Success(SearchOutcome.NotFound)
        val outcome = runNetworkFirst({ true }) { repository.refreshByNameOrId(normalized) }
        return when (outcome) {
            FetchOutcome.Fetched -> readFound(normalized, isOffline = false)
            FetchOutcome.Offline -> when (val read = repository.observeByNameOrId(normalized).first()) {
                is DataResult.Success -> AppResult.Success(SearchOutcome.Found(read.value, isOffline = true))
                // The cache has no such Pokémon, but offline we cannot know whether it exists.
                is DataResult.Failure ->
                    if (read.error == DataError.NotFound) {
                        AppResult.Failure(AppError.NoConnection)
                    } else {
                        AppResult.Failure(read.error.toAppError())
                    }
            }
            is FetchOutcome.Failed ->
                if (outcome.error == DataError.NotFound) {
                    AppResult.Success(SearchOutcome.NotFound)
                } else {
                    AppResult.Failure(outcome.error.toAppError())
                }
        }
    }

    private suspend fun readFound(query: String, isOffline: Boolean): AppResult<SearchOutcome> =
        when (val read = repository.observeByNameOrId(query).first()) {
            is DataResult.Success -> AppResult.Success(SearchOutcome.Found(read.value, isOffline))
            is DataResult.Failure -> AppResult.Failure(read.error.toAppError())
        }
}

internal class ObservePokemonDetailUseCaseImpl @Inject constructor(
    private val repository: PokemonRepository,
) : ObservePokemonDetailUseCase {
    override fun invoke(id: Int): Flow<AppResult<DataSnapshot<PokemonDetail>>> = networkFirst(
        isOnline = { true },
        fetch = { repository.refreshDetail(id) },
        observe = { repository.observeDetail(id) },
    )
}

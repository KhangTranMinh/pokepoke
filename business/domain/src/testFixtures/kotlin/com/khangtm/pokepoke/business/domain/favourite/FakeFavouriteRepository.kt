package com.khangtm.pokepoke.business.domain.favourite

import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update

/**
 * In-memory [FavouriteRepository] with the same rules as the Room implementation in `:business:data`:
 * newest first, `toggle` returns the new state, `added_at` comes from [clock].
 * [failWith] (not null) makes every function return that failure and leaves the storage unchanged.
 */
class FakeFavouriteRepository(
    private val clock: () -> Long = { System.currentTimeMillis() },
) : FavouriteRepository {

    private val entries = MutableStateFlow<List<FavouriteEntry>>(emptyList())
    private val failure = MutableStateFlow<DataError?>(null)

    var failWith: DataError?
        get() = failure.value
        set(value) {
            failure.value = value
        }

    /** Writes the storage directly; [newestFirst] is kept in the given order. */
    fun seed(newestFirst: List<FavouriteEntry>) {
        entries.value = newestFirst
    }

    private fun <T> observe(read: (List<FavouriteEntry>) -> T): Flow<DataResult<T>> =
        combine(entries, failure) { current, error ->
            if (error != null) DataResult.Failure(error) else DataResult.Success(read(current))
        }.distinctUntilChanged()

    override fun observeFavourites(): Flow<DataResult<List<FavouriteEntry>>> = observe { it }

    override fun observeIsFavourite(pokemonId: Int): Flow<DataResult<Boolean>> =
        observe { list -> list.any { it.pokemonId == pokemonId } }

    override suspend fun toggle(pokemonId: Int): DataResult<Boolean> {
        failure.value?.let { return DataResult.Failure(it) }
        var added = false
        entries.update { list ->
            if (list.any { it.pokemonId == pokemonId }) {
                added = false
                list.filterNot { it.pokemonId == pokemonId }
            } else {
                added = true
                listOf(FavouriteEntry(pokemonId, clock())) + list
            }
        }
        return DataResult.Success(added)
    }

    override suspend fun remove(pokemonId: Int): DataResult<Unit> {
        failure.value?.let { return DataResult.Failure(it) }
        entries.update { list -> list.filterNot { it.pokemonId == pokemonId } }
        return DataResult.Success(Unit)
    }

    override suspend fun clear(): DataResult<Unit> {
        failure.value?.let { return DataResult.Failure(it) }
        entries.value = emptyList()
        return DataResult.Success(Unit)
    }
}

package com.khangtm.pokepoke.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Outcome of the one-shot fetch step of [networkFirst]. */
sealed interface FetchOutcome {
    /** Fetch and save succeeded. */
    data object Fetched : FetchOutcome

    /** Confirmed offline: use the cache. */
    data object Offline : FetchOutcome

    /** Any other error: report it. */
    data class Failed(val error: DataError) : FetchOutcome
}

/**
 * `isOnline() == false` → [FetchOutcome.Offline] (fetch is not called).
 * `fetch()` success → Fetched; `Failure(Network.NoConnection | Network.Timeout)` → Offline; other failure → Failed.
 */
suspend fun runNetworkFirst(isOnline: () -> Boolean, fetch: suspend () -> DataResult<Unit>): FetchOutcome {
    if (!isOnline()) return FetchOutcome.Offline
    return when (val result = fetch()) {
        is DataResult.Success -> FetchOutcome.Fetched
        is DataResult.Failure -> when (result.error) {
            DataError.Network.NoConnection, DataError.Network.Timeout -> FetchOutcome.Offline
            else -> FetchOutcome.Failed(result.error)
        }
    }
}

/** Data together with its origin: [isOffline] is true when read from the cache because there is no network. */
data class DataSnapshot<out T>(val value: T, val isOffline: Boolean)

/**
 * Runs [runNetworkFirst], and only then collects [observe], so the UI receives exactly one first emission per load:
 * - Fetched: `Success(v)` → `Success(DataSnapshot(v, false))`; `Failure(e)` → `Failure(e.toAppError())`
 * - Offline: `Success(v)` → `Success(DataSnapshot(v, true))`; `Failure(NotFound)` → `Failure(NoConnection)`;
 *   other `Failure(e)` → `Failure(e.toAppError())`
 * - Failed(e): emits `Failure(e.toAppError())`, does not read the cache, emits nothing more.
 * After the first emission it keeps following [observe] (offline flag unchanged).
 */
fun <T> networkFirst(
    isOnline: () -> Boolean,
    fetch: suspend () -> DataResult<Unit>,
    observe: () -> Flow<DataResult<T>>,
): Flow<AppResult<DataSnapshot<T>>> = flow {
    when (val outcome = runNetworkFirst(isOnline, fetch)) {
        FetchOutcome.Fetched -> emitAll(
            observe().map { result ->
                when (result) {
                    is DataResult.Success -> AppResult.Success(DataSnapshot(result.value, isOffline = false))
                    is DataResult.Failure -> AppResult.Failure(result.error.toAppError())
                }
            },
        )

        FetchOutcome.Offline -> emitAll(
            observe().map { result ->
                when (result) {
                    is DataResult.Success -> AppResult.Success(DataSnapshot(result.value, isOffline = true))
                    is DataResult.Failure -> when (result.error) {
                        DataError.NotFound -> AppResult.Failure(AppError.NoConnection)
                        else -> AppResult.Failure(result.error.toAppError())
                    }
                }
            },
        )

        is FetchOutcome.Failed -> emit(AppResult.Failure(outcome.error.toAppError()))
    }
}

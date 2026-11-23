package com.khangtm.pokepoke.core.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

/** Error of the data layer (Repository). Translation table: docs/design/layers.md §2. */
sealed interface DataError {
    sealed interface Network : DataError {
        /** IOException (not a timeout), or the network monitor reports offline. */
        data object NoConnection : Network

        /** SocketTimeoutException. */
        data object Timeout : Network

        /** HTTP status other than 2xx and 404. */
        data class Http(val code: Int) : Network

        /** SerializationException, or a DTO → model mapping failure. */
        data class Parse(val cause: Throwable) : Network
    }

    sealed interface Local : DataError {
        data class Read(val cause: Throwable) : Local
        data class Write(val cause: Throwable) : Local
    }

    /** HTTP 404, or no record in storage. */
    data object NotFound : DataError

    data class Unexpected(val cause: Throwable) : DataError
}

/** Result of a Repository call. */
sealed interface DataResult<out T> {
    data class Success<T>(val value: T) : DataResult<T>
    data class Failure(val error: DataError) : DataResult<Nothing>
}

/**
 * The only way a Repository calls a data source one-shot: catches every [Throwable]
 * (rethrowing [CancellationException]) and returns `Failure(mapError(e))`.
 */
suspend inline fun <T> dataResultOf(mapError: (Throwable) -> DataError, block: () -> T): DataResult<T> =
    try {
        DataResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        DataResult.Failure(mapError(e))
    }

private const val INITIAL_BACKOFF_MS = 1_000L
private const val MAX_BACKOFF_MS = 30_000L
private const val MAX_BACKOFF_SHIFT = 5

/** Backoff after the n-th consecutive failure (0-based): 1s, 2s, 4s, 8s, 16s, then 30s. */
internal fun backoffMillis(consecutiveFailures: Int): Long =
    (INITIAL_BACKOFF_MS shl consecutiveFailures.coerceIn(0, MAX_BACKOFF_SHIFT)).coerceAtMost(MAX_BACKOFF_MS)

/**
 * Maps every value to `Success`. When the upstream throws, emits `Failure(mapError(e))`, waits for a
 * backoff (1s, 2s, 4s… up to 30s; back to 1s after a `Success`) and **subscribes to the upstream again**.
 * The returned flow never completes because of an error. [CancellationException] is rethrown.
 */
fun <T> Flow<T>.asDataResult(mapError: (Throwable) -> DataError): Flow<DataResult<T>> {
    val upstream = this
    return flow {
        var consecutiveFailures = 0
        emitAll(
            upstream
                .map<T, DataResult<T>> { value ->
                    consecutiveFailures = 0
                    DataResult.Success(value)
                }
                .retryWhen { cause, _ ->
                    if (cause is CancellationException) throw cause
                    emit(DataResult.Failure(mapError(cause)))
                    delay(backoffMillis(consecutiveFailures))
                    consecutiveFailures++
                    true
                },
        )
    }
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(value))
    is DataResult.Failure -> this
}

inline fun <T, R> DataResult<T>.flatMap(transform: (T) -> DataResult<R>): DataResult<R> = when (this) {
    is DataResult.Success -> transform(value)
    is DataResult.Failure -> this
}

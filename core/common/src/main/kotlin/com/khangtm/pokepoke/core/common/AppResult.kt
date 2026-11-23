package com.khangtm.pokepoke.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Error of the domain layer (UseCase), shown to the user by the UI. */
sealed interface AppError {
    /** No network, or timeout. */
    data object NoConnection : AppError

    /** HTTP 404, or no data. */
    data object NotFound : AppError

    /** HTTP status other than 2xx and 404. */
    data class Server(val code: Int) : AppError

    /** Parse error, storage error, anything else. */
    data class Unknown(val cause: Throwable? = null) : AppError
}

/** Result of a UseCase. */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

fun DataError.toAppError(): AppError = when (this) {
    DataError.Network.NoConnection, DataError.Network.Timeout -> AppError.NoConnection
    DataError.NotFound -> AppError.NotFound
    is DataError.Network.Http -> AppError.Server(code)
    is DataError.Network.Parse -> AppError.Unknown(cause)
    is DataError.Local.Read -> AppError.Unknown(cause)
    is DataError.Local.Write -> AppError.Unknown(cause)
    is DataError.Unexpected -> AppError.Unknown(cause)
}

inline fun <T> DataResult<T>.toAppResult(mapError: (DataError) -> AppError = { it.toAppError() }): AppResult<T> =
    when (this) {
        is DataResult.Success -> AppResult.Success(value)
        is DataResult.Failure -> AppResult.Failure(mapError(error))
    }

fun <T> Flow<DataResult<T>>.toAppResult(mapError: (DataError) -> AppError = { it.toAppError() }): Flow<AppResult<T>> =
    map { it.toAppResult(mapError) }

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(value)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

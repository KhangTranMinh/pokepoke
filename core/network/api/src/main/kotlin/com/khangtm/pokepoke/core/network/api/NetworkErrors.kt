package com.khangtm.pokepoke.core.network.api

import com.khangtm.pokepoke.core.common.DataError
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

private const val HTTP_NOT_FOUND = 404

/**
 * Use in a Repository that calls Retrofit: `dataResultOf(Throwable::toNetworkDataError) { api.xxx() }`.
 *
 * - [HttpException] 404 → `NotFound`; other codes → `Network.Http(code)`
 * - [SocketTimeoutException] → `Network.Timeout`; other [IOException] → `Network.NoConnection`
 * - [SerializationException], [IllegalArgumentException] → `Network.Parse(e)`
 * - anything else → `Unexpected(e)`
 */
fun Throwable.toNetworkDataError(): DataError = when (this) {
    is HttpException -> if (code() == HTTP_NOT_FOUND) DataError.NotFound else DataError.Network.Http(code())
    is SocketTimeoutException -> DataError.Network.Timeout
    is IOException -> DataError.Network.NoConnection
    is SerializationException, is IllegalArgumentException -> DataError.Network.Parse(this)
    else -> DataError.Unexpected(this)
}

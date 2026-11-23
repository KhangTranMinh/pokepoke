package com.khangtm.pokepoke.core.network.api

import com.khangtm.pokepoke.core.common.DataError
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.SocketTimeoutException

private const val HTTP_NOT_FOUND = 404

/**
 * Failure of a request that got an HTTP response with a status other than 2xx.
 *
 * `:core:network:impl` turns every non-2xx response into this exception (an `IOException` subclass, so OkHttp
 * lets it through unchanged), which keeps this module and the callers free of any HTTP library type.
 */
class NetworkHttpException(val code: Int) : IOException("HTTP $code")

/**
 * Use in a Repository that calls a remote API: `dataResultOf(Throwable::toNetworkDataError) { api.xxx() }`.
 *
 * - [NetworkHttpException] 404 → `NotFound`; other codes → `Network.Http(code)`
 * - [SocketTimeoutException] → `Network.Timeout`; other [IOException] → `Network.NoConnection`
 * - [SerializationException], [IllegalArgumentException] → `Network.Parse(e)`
 * - anything else → `Unexpected(e)`
 */
fun Throwable.toNetworkDataError(): DataError = when (this) {
    is NetworkHttpException -> if (code == HTTP_NOT_FOUND) DataError.NotFound else DataError.Network.Http(code)
    is SocketTimeoutException -> DataError.Network.Timeout
    is IOException -> DataError.Network.NoConnection
    is SerializationException, is IllegalArgumentException -> DataError.Network.Parse(this)
    else -> DataError.Unexpected(this)
}

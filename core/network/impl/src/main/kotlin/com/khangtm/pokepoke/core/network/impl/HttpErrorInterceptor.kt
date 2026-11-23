package com.khangtm.pokepoke.core.network.impl

import com.khangtm.pokepoke.core.network.api.NetworkHttpException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Turns every non-2xx response into [NetworkHttpException], so callers (and `toNetworkDataError()`) never need
 * to know the HTTP library. It is the **outermost** interceptor: interceptors contributed through
 * `@AppInterceptors` (e.g. the debug network log) still see the real response, including its status and body.
 */
internal object HttpErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.isSuccessful) return response
        val code = response.code
        response.close()
        throw NetworkHttpException(code)
    }
}

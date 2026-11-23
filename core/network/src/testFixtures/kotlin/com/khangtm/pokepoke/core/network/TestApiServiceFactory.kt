package com.khangtm.pokepoke.core.network

import okhttp3.Interceptor

/**
 * A real [ApiServiceFactory] pointed at [baseUrl] (typically a `MockWebServer`), built with the same Json, OkHttp
 * and Retrofit configuration as production. Use it in Repository tests of features:
 * `testImplementation(testFixtures(projects.core.network))`.
 */
fun createTestApiServiceFactory(
    baseUrl: String,
    interceptors: Set<Interceptor> = emptySet(),
): ApiServiceFactory = ApiServiceFactoryImpl(createRetrofit(baseUrl, createOkHttpClient(interceptors), createJson()))

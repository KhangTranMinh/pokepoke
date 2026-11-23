package com.khangtm.pokepoke.core.network

import javax.inject.Qualifier

/** Base URL of the API, provided by `:app`: `@Provides @BaseUrl fun baseUrl(): String = "https://pokeapi.co/api/v2/"`. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

/** Extra OkHttp interceptors (e.g. from `:debugtool`). `:core:network:impl` declares an empty `@Multibinds Set<Interceptor>`. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppInterceptors

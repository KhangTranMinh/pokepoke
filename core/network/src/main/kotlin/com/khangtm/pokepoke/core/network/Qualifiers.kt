package com.khangtm.pokepoke.core.network

import javax.inject.Qualifier

/** Base URL of the API, provided by `:app` from `BuildConfig.BASE_URL` (per flavor, set in `configs/env/config.toml`); never hardcoded. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

/** Extra OkHttp interceptors (e.g. from `:debugtool`). `:core:network:impl` declares an empty `@Multibinds Set<Interceptor>`. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppInterceptors

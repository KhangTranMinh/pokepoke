package com.khangtm.pokepoke.core.network.api

/** Creates Retrofit services sharing the app's HTTP client and JSON configuration. */
interface ApiServiceFactory {
    fun <T : Any> create(service: Class<T>): T
}

inline fun <reified T : Any> ApiServiceFactory.create(): T = create(T::class.java)

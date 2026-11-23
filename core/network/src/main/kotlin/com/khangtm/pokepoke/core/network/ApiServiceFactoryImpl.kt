package com.khangtm.pokepoke.core.network

import com.khangtm.pokepoke.core.network.ApiServiceFactory
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ApiServiceFactoryImpl @Inject constructor(
    private val retrofit: Retrofit,
) : ApiServiceFactory {
    override fun <T : Any> create(service: Class<T>): T = retrofit.create(service)
}

package com.khangtm.pokepoke.core.network.impl.di

import com.khangtm.pokepoke.core.network.api.ApiServiceFactory
import com.khangtm.pokepoke.core.network.api.AppInterceptors
import com.khangtm.pokepoke.core.network.api.BaseUrl
import com.khangtm.pokepoke.core.network.api.NetworkMonitor
import com.khangtm.pokepoke.core.network.impl.ApiServiceFactoryImpl
import com.khangtm.pokepoke.core.network.impl.NetworkMonitorImpl
import com.khangtm.pokepoke.core.network.impl.createJson
import com.khangtm.pokepoke.core.network.impl.createOkHttpClient
import com.khangtm.pokepoke.core.network.impl.createRetrofit
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkModule {

    @Binds
    abstract fun bindApiServiceFactory(impl: ApiServiceFactoryImpl): ApiServiceFactory

    @Binds
    abstract fun bindNetworkMonitor(impl: NetworkMonitorImpl): NetworkMonitor

    /** Empty by default; `:debugtool` (debug builds only) contributes with `@IntoSet`. */
    @Multibinds
    @AppInterceptors
    abstract fun interceptors(): Set<Interceptor>

    companion object {
        @Provides
        @Singleton
        fun provideJson(): Json = createJson()

        @Provides
        @Singleton
        fun provideOkHttpClient(@AppInterceptors interceptors: Set<@JvmSuppressWildcards Interceptor>): OkHttpClient =
            createOkHttpClient(interceptors)

        @Provides
        @Singleton
        fun provideRetrofit(@BaseUrl baseUrl: String, client: OkHttpClient, json: Json): Retrofit =
            createRetrofit(baseUrl, client, json)
    }
}

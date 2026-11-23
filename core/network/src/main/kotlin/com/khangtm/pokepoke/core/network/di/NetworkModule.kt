package com.khangtm.pokepoke.core.network.di

import com.khangtm.pokepoke.core.network.ApiServiceFactory
import com.khangtm.pokepoke.core.network.AppInterceptors
import com.khangtm.pokepoke.core.network.BaseUrl
import com.khangtm.pokepoke.core.network.ApiServiceFactoryImpl
import com.khangtm.pokepoke.core.network.createJson
import com.khangtm.pokepoke.core.network.createOkHttpClient
import com.khangtm.pokepoke.core.network.createRetrofit
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

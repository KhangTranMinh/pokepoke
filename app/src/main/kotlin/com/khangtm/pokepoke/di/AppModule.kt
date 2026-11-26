package com.khangtm.pokepoke.di

import com.khangtm.pokepoke.BuildConfig
import com.khangtm.pokepoke.core.deeplink.DeepLinkConfig
import com.khangtm.pokepoke.core.network.BaseUrl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {

    /** Per flavor (docs/design/build.md §4a). */
    @Provides
    @BaseUrl
    fun provideBaseUrl(): String = BuildConfig.BASE_URL

    @Provides
    fun provideDeepLinkConfig(): DeepLinkConfig = APP_DEEP_LINK_CONFIG
}

/** Custom scheme `pokepoke://…` and app link `https://pokepoke.com/…` (docs/design/deeplinks.md §1). */
internal val APP_DEEP_LINK_CONFIG = DeepLinkConfig(schemes = setOf("pokepoke"), hosts = setOf("pokepoke.com"))

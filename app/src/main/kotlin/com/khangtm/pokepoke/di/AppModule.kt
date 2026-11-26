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

    /** Per flavor, from `configs/env/config.toml` (docs/design/build.md §4b). */
    @Provides
    @BaseUrl
    fun provideBaseUrl(): String = BuildConfig.BASE_URL

    /** Scheme / host per flavor from `configs/env/config.toml` (docs/design/deeplinks.md §1, build.md §4b). */
    @Provides
    fun provideDeepLinkConfig(): DeepLinkConfig =
        DeepLinkConfig(schemes = setOf(BuildConfig.DEEPLINK_SCHEME), hosts = setOf(BuildConfig.DEEPLINK_HOST))
}

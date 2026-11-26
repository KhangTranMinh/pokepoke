package com.khangtm.pokepoke.core.deeplink.di

import com.khangtm.pokepoke.core.deeplink.DeepLinkDispatcher
import com.khangtm.pokepoke.core.deeplink.DeepLinkDispatcherImpl
import com.khangtm.pokepoke.core.deeplink.DeepLinkHandler
import com.khangtm.pokepoke.core.deeplink.DeepLinkIntentFactory
import com.khangtm.pokepoke.core.deeplink.DeepLinkIntentFactoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/** Empty set so the app builds when no feature contributes; features add handlers with `@IntoSet`. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DeepLinkHandlersModule {

    @Multibinds
    abstract fun handlers(): Set<DeepLinkHandler>

    @Binds
    abstract fun bindIntentFactory(impl: DeepLinkIntentFactoryImpl): DeepLinkIntentFactory
}

/** In the activity-retained component because the dispatcher needs the activity-retained `Navigator`. */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal abstract class DeepLinkDispatcherModule {

    @Binds
    abstract fun bindDispatcher(impl: DeepLinkDispatcherImpl): DeepLinkDispatcher
}

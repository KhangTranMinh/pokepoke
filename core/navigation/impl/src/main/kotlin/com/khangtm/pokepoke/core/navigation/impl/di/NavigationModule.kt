package com.khangtm.pokepoke.core.navigation.impl.di

import com.khangtm.pokepoke.core.navigation.api.EntryProviderInstaller
import com.khangtm.pokepoke.core.navigation.api.NavigationInterceptor
import com.khangtm.pokepoke.core.navigation.api.Navigator
import com.khangtm.pokepoke.core.navigation.api.TopLevelDestination
import com.khangtm.pokepoke.core.navigation.impl.NavigatorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * Empty sets so the app builds when no feature contributes. Features contribute with `@IntoSet` in modules
 * installed in `SingletonComponent`; the activity-retained [Navigator] sees them through the parent component.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class NavigationMultibindsModule {

    @Multibinds
    abstract fun interceptors(): Set<NavigationInterceptor>

    @Multibinds
    abstract fun entryProviderInstallers(): Set<EntryProviderInstaller>

    @Multibinds
    abstract fun topLevelDestinations(): Set<TopLevelDestination>
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal abstract class NavigatorModule {

    @Binds
    abstract fun bindNavigator(impl: NavigatorImpl): Navigator
}

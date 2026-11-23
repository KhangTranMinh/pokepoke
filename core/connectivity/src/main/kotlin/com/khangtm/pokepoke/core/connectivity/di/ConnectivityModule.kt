package com.khangtm.pokepoke.core.connectivity.di

import com.khangtm.pokepoke.core.connectivity.NetworkMonitor
import com.khangtm.pokepoke.core.connectivity.NetworkMonitorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ConnectivityModule {

    @Binds
    abstract fun bindNetworkMonitor(impl: NetworkMonitorImpl): NetworkMonitor
}

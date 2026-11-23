package com.khangtm.pokepoke.core.storage.impl.di

import com.khangtm.pokepoke.core.storage.api.KeyValueStoreFactory
import com.khangtm.pokepoke.core.storage.impl.KeyValueStoreFactoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class StorageModule {

    @Binds
    abstract fun bindKeyValueStoreFactory(impl: KeyValueStoreFactoryImpl): KeyValueStoreFactory
}

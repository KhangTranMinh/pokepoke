package com.khangtm.pokepoke.core.securestorage.impl.di

import com.khangtm.pokepoke.core.securestorage.api.SecureStorageFactory
import com.khangtm.pokepoke.core.securestorage.impl.AeadProvider
import com.khangtm.pokepoke.core.securestorage.impl.SecureStorageFactoryImpl
import com.khangtm.pokepoke.core.securestorage.impl.TinkAeadProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SecureStorageModule {

    @Binds
    abstract fun bindSecureStorageFactory(impl: SecureStorageFactoryImpl): SecureStorageFactory

    @Binds
    abstract fun bindAeadProvider(impl: TinkAeadProvider): AeadProvider
}

package com.khangtm.pokepoke.business.data.di

import android.content.Context
import androidx.room.Room
import com.khangtm.pokepoke.business.data.pokemon.PokemonApi
import com.khangtm.pokepoke.business.data.pokemon.PokemonRepositoryImpl
import com.khangtm.pokepoke.business.data.pokemon.db.PokemonDao
import com.khangtm.pokepoke.business.data.pokemon.db.PokemonDatabase
import com.khangtm.pokepoke.business.domain.pokemon.PokemonRepository
import com.khangtm.pokepoke.core.network.ApiServiceFactory
import com.khangtm.pokepoke.core.network.create
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PokemonDataModule {

    @Binds
    abstract fun bindPokemonRepository(impl: PokemonRepositoryImpl): PokemonRepository

    companion object {
        @Provides
        @Singleton
        fun providePokemonApi(factory: ApiServiceFactory): PokemonApi = factory.create()

        @Provides
        @Singleton
        fun providePokemonDatabase(@ApplicationContext context: Context): PokemonDatabase =
            // A cache: on a schema change just rebuild it.
            Room.databaseBuilder(context, PokemonDatabase::class.java, PokemonDatabase.NAME)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        @Provides
        fun providePokemonDao(database: PokemonDatabase): PokemonDao = database.pokemonDao()
    }
}

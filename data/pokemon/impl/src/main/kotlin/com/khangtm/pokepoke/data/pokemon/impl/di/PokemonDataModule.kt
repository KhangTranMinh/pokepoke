package com.khangtm.pokepoke.data.pokemon.impl.di

import android.content.Context
import androidx.room.Room
import com.khangtm.pokepoke.data.pokemon.api.PokemonRepository
import com.khangtm.pokepoke.data.pokemon.impl.PokemonRepositoryImpl
import com.khangtm.pokepoke.data.pokemon.impl.db.PokemonDao
import com.khangtm.pokepoke.data.pokemon.impl.db.PokemonDatabase
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
        fun providePokemonDatabase(@ApplicationContext context: Context): PokemonDatabase =
            // A cache: on a schema change just rebuild it.
            Room.databaseBuilder(context, PokemonDatabase::class.java, PokemonDatabase.NAME)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        @Provides
        fun providePokemonDao(database: PokemonDatabase): PokemonDao = database.pokemonDao()
    }
}

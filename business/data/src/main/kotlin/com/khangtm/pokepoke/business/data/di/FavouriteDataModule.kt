package com.khangtm.pokepoke.business.data.di

import android.content.Context
import androidx.room.Room
import com.khangtm.pokepoke.business.data.favourite.FavouriteRepositoryImpl
import com.khangtm.pokepoke.business.data.favourite.db.FavouriteDao
import com.khangtm.pokepoke.business.data.favourite.db.FavouriteDatabase
import com.khangtm.pokepoke.business.domain.favourite.FavouriteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FavouriteDataModule {

    @Binds
    abstract fun bindFavouriteRepository(impl: FavouriteRepositoryImpl): FavouriteRepository

    companion object {
        @Provides
        @Singleton
        // User data: no destructive migration.
        fun provideFavouriteDatabase(@ApplicationContext context: Context): FavouriteDatabase =
            Room.databaseBuilder(context, FavouriteDatabase::class.java, FavouriteDatabase.NAME).build()

        @Provides
        fun provideFavouriteDao(database: FavouriteDatabase): FavouriteDao = database.favouriteDao()
    }
}

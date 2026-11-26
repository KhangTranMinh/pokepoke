package com.khangtm.pokepoke.business.data.favourite.db

import androidx.room.Database
import androidx.room.RoomDatabase

/** User data: `exportSchema = true` and no destructive migration (a schema change needs a real `Migration`). */
@Database(entities = [FavouriteEntity::class], version = 1, exportSchema = true)
internal abstract class FavouriteDatabase : RoomDatabase() {
    abstract fun favouriteDao(): FavouriteDao

    companion object {
        const val NAME = "favourite.db"
    }
}

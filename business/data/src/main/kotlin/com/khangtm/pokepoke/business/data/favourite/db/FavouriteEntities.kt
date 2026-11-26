package com.khangtm.pokepoke.business.data.favourite.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourite")
internal data class FavouriteEntity(
    @PrimaryKey @ColumnInfo(name = "pokemon_id") val pokemonId: Int,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)

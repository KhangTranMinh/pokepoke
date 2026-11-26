package com.khangtm.pokepoke.business.data.pokemon.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PokemonEntity::class, PokemonDetailEntity::class, ListMetaEntity::class],
    version = 1,
)
internal abstract class PokemonDatabase : RoomDatabase() {
    abstract fun pokemonDao(): PokemonDao

    companion object {
        const val NAME = "pokemon.db"
    }
}

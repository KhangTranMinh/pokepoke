package com.khangtm.pokepoke.business.data.pokemon.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pokemon", indices = [Index("name")])
internal data class PokemonEntity(
    @PrimaryKey val id: Int,
    val name: String,
    @ColumnInfo(name = "image_url") val imageUrl: String,
    /** Index in the API list order; null when the pokemon is not part of the list. */
    @ColumnInfo(name = "list_position") val listPosition: Int?,
)

@Entity(tableName = "pokemon_detail")
internal data class PokemonDetailEntity(
    @PrimaryKey @ColumnInfo(name = "pokemon_id") val pokemonId: Int,
    val height: Int,
    val weight: Int,
    @ColumnInfo(name = "base_experience") val baseExperience: Int?,
    @ColumnInfo(name = "types_json") val typesJson: String,
    @ColumnInfo(name = "abilities_json") val abilitiesJson: String,
    @ColumnInfo(name = "stats_json") val statsJson: String,
    @ColumnInfo(name = "evolution_json") val evolutionJson: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Single row (id is always 0). */
@Entity(tableName = "list_meta")
internal data class ListMetaEntity(
    @PrimaryKey val id: Int = 0,
    @ColumnInfo(name = "total_count") val totalCount: Int,
)

/** Result of joining `pokemon` and `pokemon_detail`. */
internal data class PokemonDetailRow(
    val id: Int,
    val name: String,
    @ColumnInfo(name = "image_url") val imageUrl: String,
    val height: Int,
    val weight: Int,
    @ColumnInfo(name = "base_experience") val baseExperience: Int?,
    @ColumnInfo(name = "types_json") val typesJson: String,
    @ColumnInfo(name = "abilities_json") val abilitiesJson: String,
    @ColumnInfo(name = "stats_json") val statsJson: String,
    @ColumnInfo(name = "evolution_json") val evolutionJson: String,
)

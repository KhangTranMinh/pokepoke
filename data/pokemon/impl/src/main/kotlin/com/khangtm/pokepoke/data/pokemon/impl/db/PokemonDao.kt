package com.khangtm.pokepoke.data.pokemon.impl.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal abstract class PokemonDao {

    @Query("SELECT * FROM pokemon WHERE list_position IS NOT NULL ORDER BY list_position ASC LIMIT :limit")
    abstract fun observeList(limit: Int): Flow<List<PokemonEntity>>

    @Query("SELECT total_count FROM list_meta WHERE id = 0")
    abstract fun observeTotalCount(): Flow<Int?>

    @Query(
        "SELECT p.id AS id, p.name AS name, p.image_url AS image_url, d.height AS height, d.weight AS weight, " +
            "d.base_experience AS base_experience, d.types_json AS types_json, d.abilities_json AS abilities_json, " +
            "d.stats_json AS stats_json, d.evolution_json AS evolution_json " +
            "FROM pokemon p INNER JOIN pokemon_detail d ON d.pokemon_id = p.id WHERE p.id = :id",
    )
    abstract fun observeDetail(id: Int): Flow<PokemonDetailRow?>

    @Query("SELECT * FROM pokemon WHERE id = :id")
    abstract fun observeById(id: Int): Flow<PokemonEntity?>

    @Query("SELECT * FROM pokemon WHERE name = :name LIMIT 1")
    abstract fun observeByName(name: String): Flow<PokemonEntity?>

    @Query("SELECT * FROM pokemon WHERE id IN (:ids)")
    abstract fun observeByIds(ids: List<Int>): Flow<List<PokemonEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertIgnore(entity: PokemonEntity)

    @Query("UPDATE pokemon SET name = :name, image_url = :imageUrl WHERE id = :id")
    protected abstract suspend fun updateNameAndImage(id: Int, name: String, imageUrl: String)

    @Query("UPDATE pokemon SET list_position = NULL WHERE list_position = :position AND id != :id")
    protected abstract suspend fun clearPositionOfOthers(position: Int, id: Int)

    @Query("UPDATE pokemon SET list_position = :position WHERE id = :id")
    protected abstract suspend fun setPosition(id: Int, position: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertDetail(detail: PokemonDetailEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertMeta(meta: ListMetaEntity)

    /** Upserts name / image only: an existing row keeps its list position and its detail. */
    @Transaction
    open suspend fun saveSummary(id: Int, name: String, imageUrl: String) {
        insertIgnore(PokemonEntity(id = id, name = name, imageUrl = imageUrl, listPosition = null))
        updateNameAndImage(id, name, imageUrl)
    }

    @Transaction
    open suspend fun saveList(items: List<PokemonEntity>, offset: Int, totalCount: Int) {
        items.forEachIndexed { index, item ->
            val position = offset + index
            saveSummary(item.id, item.name, item.imageUrl)
            clearPositionOfOthers(position, item.id)
            setPosition(item.id, position)
        }
        upsertMeta(ListMetaEntity(totalCount = totalCount))
    }

    @Transaction
    open suspend fun saveDetail(summary: PokemonEntity, detail: PokemonDetailEntity) {
        saveSummary(summary.id, summary.name, summary.imageUrl)
        upsertDetail(detail)
    }
}

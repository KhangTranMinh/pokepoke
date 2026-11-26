package com.khangtm.pokepoke.business.data.favourite.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal abstract class FavouriteDao {

    /** Newest first; `pokemon_id` breaks ties of equal `added_at` (higher id first) so the order is stable. */
    @Query("SELECT * FROM favourite ORDER BY added_at DESC, pokemon_id DESC")
    abstract fun observeAll(): Flow<List<FavouriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favourite WHERE pokemon_id = :pokemonId)")
    abstract fun observeExists(pokemonId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favourite WHERE pokemon_id = :pokemonId)")
    protected abstract suspend fun exists(pokemonId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(entity: FavouriteEntity)

    @Query("DELETE FROM favourite WHERE pokemon_id = :pokemonId")
    protected abstract suspend fun deleteRow(pokemonId: Int)

    @Query("DELETE FROM favourite")
    protected abstract suspend fun deleteRows()

    // `remove` / `clear` are transactions too: on a closed database a plain suspend query fails with a
    // CancellationException (Room cancels its scope), a transaction fails with an IllegalStateException.
    @Transaction
    open suspend fun remove(pokemonId: Int) = deleteRow(pokemonId)

    @Transaction
    open suspend fun clear() = deleteRows()

    /** Adds (with [addedAt]) or removes in one transaction; returns the new state (`true` = just added). */
    @Transaction
    open suspend fun toggle(pokemonId: Int, addedAt: Long): Boolean =
        if (exists(pokemonId)) {
            deleteRow(pokemonId)
            false
        } else {
            insert(FavouriteEntity(pokemonId, addedAt))
            true
        }
}

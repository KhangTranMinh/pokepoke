package com.khangtm.pokepoke.core.storage.api

import kotlinx.coroutines.flow.Flow

interface KeyValueStoreFactory {
    /**
     * Each [name] is a separate file ("<name>.preferences_pb"); the same name returns the same instance.
     * [name] may contain letters, digits, `_`, `-` and `.`, and must not start with `secure_` (reserved for
     * `:core:securestorage`).
     */
    fun create(name: String): KeyValueStore

    /** Names of the stores that exist on disk (for `:debugtool`). */
    fun storeNames(): List<String>
}

/**
 * Typed key-value storage. A key holds one value of one type: writing a key with another type replaces the old
 * value, and observing a key as a type it does not hold emits `null`.
 *
 * Data source: operations may throw (IO errors); only a Repository calls it, through `dataResultOf` / `asDataResult`.
 */
interface KeyValueStore {
    val name: String

    fun observeString(key: String): Flow<String?>
    fun observeBoolean(key: String): Flow<Boolean?>
    fun observeLong(key: String): Flow<Long?>
    fun observeInt(key: String): Flow<Int?>

    suspend fun putString(key: String, value: String)
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun putLong(key: String, value: Long)
    suspend fun putInt(key: String, value: Int)

    suspend fun remove(key: String)
    suspend fun clear()

    /** Every entry as `key → value` (String, Boolean, Long or Int). */
    fun observeAll(): Flow<Map<String, Any>>
}

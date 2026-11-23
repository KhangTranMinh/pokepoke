package com.khangtm.pokepoke.core.securestorage.api

import kotlinx.coroutines.flow.Flow

interface SecureStorageFactory {
    /** Each [name] is a separate encrypted store; the same name returns the same instance. */
    fun create(name: String): SecureStorage

    /** Names of the stores that exist on disk (for `:debugtool`). */
    fun storeNames(): List<String>
}

/**
 * Encrypted key-value storage of strings. Keys are visible, values are encrypted at rest.
 *
 * Data source: operations may throw (IO or decryption errors); only a Repository calls it, through
 * `dataResultOf` / `asDataResult`.
 */
interface SecureStorage {
    val name: String

    /** Current value of [key], or `null` when absent. */
    suspend fun get(key: String): String?
    fun observe(key: String): Flow<String?>

    suspend fun put(key: String, value: String)
    suspend fun remove(key: String)
    suspend fun clear()

    /** The keys that currently have a value (never the values). */
    fun observeKeys(): Flow<Set<String>>
}

package com.khangtm.pokepoke.core.securestorage.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [SecureStorage] (not encrypted), with the same rules as the real implementation. */
class FakeSecureStorage(override val name: String) : SecureStorage {

    private val entries = MutableStateFlow<Map<String, String>>(emptyMap())

    /** True once something was written (a DataStore file only exists after the first write). */
    var hasBeenWritten: Boolean = false
        private set

    /** Set to make every operation throw, to test how callers handle storage errors. */
    var failWith: Throwable? = null

    private fun check() {
        failWith?.let { throw it }
    }

    override suspend fun get(key: String): String? {
        check()
        return entries.value[key]
    }

    override fun observe(key: String): Flow<String?> = entries.map {
        check()
        it[key]
    }.distinctUntilChanged()

    override suspend fun put(key: String, value: String) {
        check()
        hasBeenWritten = true
        entries.update { it + (key to value) }
    }

    override suspend fun remove(key: String) {
        check()
        hasBeenWritten = true
        entries.update { it - key }
    }

    override suspend fun clear() {
        check()
        hasBeenWritten = true
        entries.value = emptyMap()
    }

    override fun observeKeys(): Flow<Set<String>> = entries.map {
        check()
        it.keys.toSet()
    }.distinctUntilChanged()
}

/** In-memory [SecureStorageFactory]; stores are shared by name like the real one. */
class FakeSecureStorageFactory : SecureStorageFactory {

    private val stores = linkedMapOf<String, FakeSecureStorage>()

    @Synchronized
    override fun create(name: String): FakeSecureStorage = stores.getOrPut(name) { FakeSecureStorage(name) }

    /** Only stores that were written to, like files on disk. */
    @Synchronized
    override fun storeNames(): List<String> = stores.values.filter { it.hasBeenWritten }.map { it.name }.sorted()
}

package com.khangtm.pokepoke.core.storage.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [KeyValueStore], with the same rules as the DataStore implementation. */
class FakeKeyValueStore(override val name: String) : KeyValueStore {

    private val entries = MutableStateFlow<Map<String, Any>>(emptyMap())

    /** True once something was written (a DataStore file only exists after the first write). */
    var hasBeenWritten: Boolean = false
        private set

    private inline fun <reified T : Any> observe(key: String): Flow<T?> =
        entries.map { it[key] as? T }.distinctUntilChanged()

    private fun put(key: String, value: Any) {
        hasBeenWritten = true
        entries.update { it + (key to value) }
    }

    override fun observeString(key: String): Flow<String?> = observe(key)
    override fun observeBoolean(key: String): Flow<Boolean?> = observe(key)
    override fun observeLong(key: String): Flow<Long?> = observe(key)
    override fun observeInt(key: String): Flow<Int?> = observe(key)

    override suspend fun putString(key: String, value: String) = put(key, value)
    override suspend fun putBoolean(key: String, value: Boolean) = put(key, value)
    override suspend fun putLong(key: String, value: Long) = put(key, value)
    override suspend fun putInt(key: String, value: Int) = put(key, value)

    override suspend fun remove(key: String) {
        hasBeenWritten = true
        entries.update { it - key }
    }

    override suspend fun clear() {
        hasBeenWritten = true
        entries.value = emptyMap()
    }

    override fun observeAll(): Flow<Map<String, Any>> = entries.map { it.toMap() }.distinctUntilChanged()
}

/** In-memory [KeyValueStoreFactory]; stores are shared by name like the real one. */
class FakeKeyValueStoreFactory : KeyValueStoreFactory {

    private val stores = linkedMapOf<String, FakeKeyValueStore>()

    @Synchronized
    override fun create(name: String): FakeKeyValueStore = stores.getOrPut(name) { FakeKeyValueStore(name) }

    /** Only stores that were written to, like files on disk. */
    @Synchronized
    override fun storeNames(): List<String> = stores.values.filter { it.hasBeenWritten }.map { it.name }.sorted()
}

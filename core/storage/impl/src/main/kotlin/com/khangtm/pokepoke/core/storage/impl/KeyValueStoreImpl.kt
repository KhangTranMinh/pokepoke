package com.khangtm.pokepoke.core.storage.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.khangtm.pokepoke.core.storage.api.KeyValueStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** [KeyValueStore] on top of one Preferences DataStore file. */
internal class KeyValueStoreImpl(
    override val name: String,
    private val dataStore: DataStore<Preferences>,
) : KeyValueStore {

    /**
     * `Preferences.Key` equality ignores the value type, so `preferences[intKey]` would hand back a String stored
     * under the same name. Look the value up by name and keep it only when it has the requested type.
     */
    private inline fun <reified T : Any> observe(key: String): Flow<T?> =
        dataStore.data
            .map { preferences -> preferences.asMap().entries.firstOrNull { it.key.name == key }?.value as? T }
            .distinctUntilChanged()

    private suspend fun <T : Any> put(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences.remove(key) // same name, any previous type
            preferences[key] = value
        }
    }

    override fun observeString(key: String): Flow<String?> = observe<String>(key)
    override fun observeBoolean(key: String): Flow<Boolean?> = observe<Boolean>(key)
    override fun observeLong(key: String): Flow<Long?> = observe<Long>(key)
    override fun observeInt(key: String): Flow<Int?> = observe<Int>(key)

    override suspend fun putString(key: String, value: String) = put(stringPreferencesKey(key), value)
    override suspend fun putBoolean(key: String, value: Boolean) = put(booleanPreferencesKey(key), value)
    override suspend fun putLong(key: String, value: Long) = put(longPreferencesKey(key), value)
    override suspend fun putInt(key: String, value: Int) = put(intPreferencesKey(key), value)

    override suspend fun remove(key: String) {
        dataStore.edit { preferences ->
            preferences.asMap().keys.filter { it.name == key }.forEach { preferences.remove(it) }
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    override fun observeAll(): Flow<Map<String, Any>> =
        dataStore.data.map { preferences -> preferences.asMap().mapKeys { it.key.name } }.distinctUntilChanged()
}

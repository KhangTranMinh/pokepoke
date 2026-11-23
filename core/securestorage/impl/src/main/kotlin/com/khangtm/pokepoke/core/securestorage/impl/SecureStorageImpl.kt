package com.khangtm.pokepoke.core.securestorage.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.Aead
import com.khangtm.pokepoke.core.securestorage.api.SecureStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Base64

/**
 * Stored value = Base64(`aead.encrypt(value.utf8, associatedData = key.utf8)`). Binding the key as associated data
 * means a ciphertext copied under another key cannot be decrypted.
 */
internal class SecureStorageImpl(
    override val name: String,
    private val dataStore: DataStore<Preferences>,
    private val aeadProvider: AeadProvider,
) : SecureStorage {

    private fun encrypt(key: String, value: String): String {
        val ciphertext = aeadProvider.aead().encrypt(value.toByteArray(Charsets.UTF_8), key.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(ciphertext)
    }

    private fun decrypt(key: String, stored: String): String {
        val plaintext = aeadProvider.aead().decrypt(Base64.getDecoder().decode(stored), key.toByteArray(Charsets.UTF_8))
        return String(plaintext, Charsets.UTF_8)
    }

    private fun Preferences.read(key: String): String? = this[stringPreferencesKey(key)]?.let { decrypt(key, it) }

    override suspend fun get(key: String): String? = dataStore.data.first().read(key)

    override fun observe(key: String): Flow<String?> = dataStore.data.map { it.read(key) }.distinctUntilChanged()

    override suspend fun put(key: String, value: String) {
        val encrypted = encrypt(key, value)
        dataStore.edit { it[stringPreferencesKey(key)] = encrypted }
    }

    override suspend fun remove(key: String) {
        dataStore.edit { it.remove(stringPreferencesKey(key)) }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    override fun observeKeys(): Flow<Set<String>> =
        dataStore.data.map { preferences -> preferences.asMap().keys.map { it.name }.toSet() }.distinctUntilChanged()
}

package com.khangtm.pokepoke.core.securestorage.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.khangtm.pokepoke.core.securestorage.api.SecureStorageContract
import com.khangtm.pokepoke.core.securestorage.api.SecureStorageFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.security.GeneralSecurityException
import java.util.Base64

/** The encrypted implementation must follow the same contract as the fake. */
@RunWith(RobolectricTestRunner::class)
class SecureStorageFactoryImplContractTest : SecureStorageContract() {

    @get:Rule
    val folder = TemporaryFolder()

    override fun createFactory(scope: CoroutineScope): SecureStorageFactory =
        SecureStorageFactoryImpl(folder.newFolder(), scope, TestAeadProvider())
}

@RunWith(RobolectricTestRunner::class)
class SecureStorageImplTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val aeadProvider = TestAeadProvider()

    private fun TestScope.rawStore(file: File): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { file }

    private fun TestScope.storage(name: String = "auth", dataStore: DataStore<Preferences> = rawStore(folder.newFile("$name.preferences_pb").also { it.delete() })) =
        SecureStorageImpl(name, dataStore, aeadProvider) to dataStore

    private suspend fun DataStore<Preferences>.raw(key: String): String? = data.first()[stringPreferencesKey(key)]

    @Test
    fun `stored value is not the plaintext`() = runTest {
        val (storage, dataStore) = storage()
        storage.put("token", "super-secret-token")

        val raw = dataStore.raw("token")
        assertNotNull(raw)
        assertNotEquals("super-secret-token", raw)
        assertFalse(raw!!.contains("super-secret"))
    }

    @Test
    fun `stored value is Base64 of ciphertext that is longer than the plaintext`() = runTest {
        val (storage, dataStore) = storage()
        storage.put("token", "abc")

        val bytes = Base64.getDecoder().decode(dataStore.raw("token"))
        assertTrue("nonce + tag make it longer", bytes.size > "abc".length)
    }

    @Test
    fun `the file on disk contains no plaintext`() = runTest {
        val directory = folder.newFolder()
        val factory = SecureStorageFactoryImpl(directory, backgroundScope, aeadProvider)
        factory.create("auth").put("token", "super-secret-token")

        val bytes = File(directory, "secure_auth.preferences_pb").readBytes()
        assertTrue(bytes.isNotEmpty())
        assertFalse(String(bytes, Charsets.ISO_8859_1).contains("super-secret-token"))
        assertTrue("keys stay visible", String(bytes, Charsets.ISO_8859_1).contains("token"))
    }

    @Test
    fun `the same value is encrypted differently each time`() = runTest {
        val (storage, dataStore) = storage()
        storage.put("k", "same")
        val first = dataStore.raw("k")
        storage.put("k", "same")
        val second = dataStore.raw("k")

        assertNotEquals(first, second)
        assertEquals("same", storage.get("k"))
    }

    @Test
    fun `a ciphertext moved to another key cannot be decrypted`() = runTest {
        val (storage, dataStore) = storage()
        storage.put("access", "access-token")
        val stolen = dataStore.raw("access")!!
        dataStore.edit { it[stringPreferencesKey("refresh")] = stolen }

        assertEquals("access-token", storage.get("access"))
        try {
            storage.get("refresh")
            fail("Expected decryption to fail because the key is the associated data")
        } catch (expected: GeneralSecurityException) {
            // ok
        }
    }

    @Test
    fun `tampered or malformed stored values throw`() = runTest {
        val (storage, dataStore) = storage()
        storage.put("k", "value")
        val bytes = Base64.getDecoder().decode(dataStore.raw("k")).also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        dataStore.edit { it[stringPreferencesKey("k")] = Base64.getEncoder().encodeToString(bytes) }

        try {
            storage.get("k")
            fail("Expected tampered ciphertext to be rejected")
        } catch (expected: GeneralSecurityException) {
            // ok
        }

        dataStore.edit { it[stringPreferencesKey("k")] = "not base64 !!!" }
        try {
            storage.get("k")
            fail("Expected malformed value to be rejected")
        } catch (expected: IllegalArgumentException) {
            // ok
        }
    }

    @Test
    fun `values written by another storage instance with the same key can be read`() = runTest {
        val file = folder.newFile("shared.preferences_pb").also { it.delete() }
        val dataStore = rawStore(file)
        SecureStorageImpl("a", dataStore, aeadProvider).put("k", "v")
        assertEquals("v", SecureStorageImpl("b", dataStore, aeadProvider).get("k"))
    }

    // ---- factory ----

    @Test
    fun `each name is stored in its own secure_ file`() = runTest {
        val directory = folder.newFolder()
        val factory = SecureStorageFactoryImpl(directory, backgroundScope, aeadProvider)
        factory.create("auth").put("k", "v")

        assertTrue(File(directory, "secure_auth.preferences_pb").exists())
        assertFalse(File(directory, "auth.preferences_pb").exists())
    }

    @Test
    fun `storeNames lists secure files only without the prefix`() = runTest {
        val directory = folder.newFolder()
        File(directory, "secure_auth.preferences_pb").writeText("x")
        File(directory, "secure_profile.preferences_pb").writeText("x")
        File(directory, "plain.preferences_pb").writeText("x")
        File(directory, "secure_notes.txt").writeText("x")

        val factory = SecureStorageFactoryImpl(directory, backgroundScope, aeadProvider)

        assertEquals(listOf("auth", "profile"), factory.storeNames())
    }

    @Test
    fun `storeNames is empty when the directory does not exist yet`() = runTest {
        val factory = SecureStorageFactoryImpl(File(folder.root, "missing"), backgroundScope, aeadProvider)
        assertTrue(factory.storeNames().isEmpty())
    }

    @Test
    fun `invalid names are rejected`() = runTest {
        val factory = SecureStorageFactoryImpl(folder.newFolder(), backgroundScope, aeadProvider)
        listOf("", "a/b", "../escape", "with space").forEach { name ->
            try {
                factory.create(name)
                fail("Expected '$name' to be rejected")
            } catch (expected: IllegalArgumentException) {
                // ok
            }
        }
    }

    @Test
    fun `the Hilt constructor stores files under filesDir slash datastore`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val factory = SecureStorageFactoryImpl(context, Dispatchers.Unconfined, aeadProvider)
        factory.create("hilt-path").put("k", "v")

        assertTrue(File(context.filesDir, "datastore/secure_hilt-path.preferences_pb").exists())
        assertTrue("hilt-path" in factory.storeNames())
    }
}

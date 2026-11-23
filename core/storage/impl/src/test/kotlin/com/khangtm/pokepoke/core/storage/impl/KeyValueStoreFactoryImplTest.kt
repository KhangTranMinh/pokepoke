package com.khangtm.pokepoke.core.storage.impl

import androidx.test.core.app.ApplicationProvider
import com.khangtm.pokepoke.core.storage.api.KeyValueStoreContract
import com.khangtm.pokepoke.core.storage.api.KeyValueStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** The DataStore implementation must follow the same contract as the fake. */
@RunWith(RobolectricTestRunner::class)
class KeyValueStoreFactoryImplContractTest : KeyValueStoreContract() {

    @get:Rule
    val folder = TemporaryFolder()

    override fun createFactory(scope: CoroutineScope): KeyValueStoreFactory =
        KeyValueStoreFactoryImpl(folder.newFolder(), scope)
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class KeyValueStoreFactoryImplTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `each name is stored in its own preferences_pb file`() = runTest {
        val directory = folder.newFolder()
        val factory = KeyValueStoreFactoryImpl(directory, backgroundScope)
        factory.create("alpha").putString("k", "v")

        assertTrue(File(directory, "alpha.preferences_pb").exists())
        assertFalse(File(directory, "beta.preferences_pb").exists())
    }

    @Test
    fun `data survives a new factory on the same directory`() = runTest {
        val directory = folder.newFolder()
        val firstScope = CoroutineScope(Job() + UnconfinedTestDispatcher(testScheduler))
        KeyValueStoreFactoryImpl(directory, firstScope).create("persist").putInt("count", 42)
        firstScope.coroutineContext[Job]?.let { it.cancel(); it.join() }

        val second = KeyValueStoreFactoryImpl(directory, backgroundScope)
        assertEquals(42, second.create("persist").observeInt("count").first())
    }

    @Test
    fun `storeNames lists files on disk and skips secure files and other files`() = runTest {
        val directory = folder.newFolder()
        File(directory, "alpha.preferences_pb").writeText("x")
        File(directory, "beta.preferences_pb").writeText("x")
        File(directory, "secure_tokens.preferences_pb").writeText("x")
        File(directory, "notes.txt").writeText("x")

        val factory = KeyValueStoreFactoryImpl(directory, backgroundScope)

        assertEquals(listOf("alpha", "beta"), factory.storeNames())
    }

    @Test
    fun `storeNames is empty when the directory does not exist yet`() = runTest {
        val factory = KeyValueStoreFactoryImpl(File(folder.root, "missing"), backgroundScope)
        assertTrue(factory.storeNames().isEmpty())
    }

    @Test
    fun `invalid and reserved names are rejected`() = runTest {
        val factory = KeyValueStoreFactoryImpl(folder.newFolder(), backgroundScope)
        listOf("", "a/b", "../escape", "with space", "secure_tokens").forEach { name ->
            try {
                factory.create(name)
                fail("Expected '$name' to be rejected")
            } catch (expected: IllegalArgumentException) {
                // ok
            }
        }
    }

    @Test
    fun `a key written as another type is fully replaced on disk`() = runTest {
        val store = KeyValueStoreFactoryImpl(folder.newFolder(), backgroundScope).create("types")
        store.putString("k", "text")
        store.putLong("k", 5L)

        assertNull(store.observeString("k").first())
        assertEquals(5L, store.observeLong("k").first())
        assertEquals(1, store.observeAll().first().size)
    }

    @Test
    fun `the Hilt constructor stores files under filesDir slash datastore`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val factory = KeyValueStoreFactoryImpl(context, Dispatchers.Unconfined)
        factory.create("hilt-path").putBoolean("flag", true)

        assertTrue(File(context.filesDir, "datastore/hilt-path.preferences_pb").exists())
        assertTrue("hilt-path" in factory.storeNames())
    }
}

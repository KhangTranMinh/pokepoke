package com.khangtm.pokepoke.core.securestorage.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rules every [SecureStorageFactory] must follow. Extend it for the real implementation and for the fake, so both
 * are verified against the same tests. [createFactory] must return a factory whose background work runs in [scope].
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class SecureStorageContract {

    abstract fun createFactory(scope: CoroutineScope): SecureStorageFactory

    @Test
    fun `missing key is null`() = runTest {
        val storage = createFactory(backgroundScope).create("contract")
        assertNull(storage.get("k"))
        assertNull(storage.observe("k").first())
    }

    @Test
    fun `put then get and observe return the value`() = runTest {
        val storage = createFactory(backgroundScope).create("contract")
        storage.put("token", "abc.def.ghi")
        assertEquals("abc.def.ghi", storage.get("token"))
        assertEquals("abc.def.ghi", storage.observe("token").first())
    }

    @Test
    fun `put overwrites the previous value`() = runTest {
        val storage = createFactory(backgroundScope).create("contract")
        storage.put("k", "one")
        storage.put("k", "two")
        assertEquals("two", storage.get("k"))
    }

    @Test
    fun `empty and unicode values round trip`() = runTest {
        val storage = createFactory(backgroundScope).create("contract")
        storage.put("empty", "")
        storage.put("unicode", "Pokémon — Tiếng Việt 🐞")
        assertEquals("", storage.get("empty"))
        assertEquals("Pokémon — Tiếng Việt 🐞", storage.get("unicode"))
    }

    @Test
    fun `remove deletes only that key`() = runTest {
        val storage = createFactory(backgroundScope).create("contract")
        storage.put("a", "1")
        storage.put("b", "2")
        storage.remove("a")
        storage.remove("never-existed")
        assertNull(storage.get("a"))
        assertEquals("2", storage.get("b"))
    }

    @Test
    fun `clear deletes every key`() = runTest {
        val storage = createFactory(backgroundScope).create("contract")
        storage.put("a", "1")
        storage.put("b", "2")
        storage.clear()
        assertNull(storage.get("a"))
        assertTrue(storage.observeKeys().first().isEmpty())
    }

    @Test
    fun `observeKeys lists keys without values`() = runTest {
        val storage = createFactory(backgroundScope).create("contract")
        assertTrue(storage.observeKeys().first().isEmpty())
        storage.put("access", "secret-1")
        storage.put("refresh", "secret-2")
        assertEquals(setOf("access", "refresh"), storage.observeKeys().first())
    }

    @Test
    fun `observe emits later changes`() = runTest {
        val storage = createFactory(backgroundScope).create("contract")
        val seen = mutableListOf<String?>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            storage.observe("k").collect { seen += it }
        }
        advanceUntilIdle()

        storage.put("k", "one")
        advanceUntilIdle()
        storage.put("k", "two")
        advanceUntilIdle()
        storage.remove("k")
        advanceUntilIdle()
        collector.cancel()

        assertEquals(listOf(null, "one", "two", null), seen)
    }

    @Test
    fun `same name returns the same instance and different names are isolated`() = runTest {
        val factory = createFactory(backgroundScope)
        val first = factory.create("one")
        assertSame(first, factory.create("one"))
        assertEquals("one", first.name)

        first.put("k", "value")
        assertNull(factory.create("two").get("k"))
    }

    @Test
    fun `storeNames lists stores that were written to`() = runTest {
        val factory = createFactory(backgroundScope)
        factory.create("auth").put("k", "v")
        factory.create("profile").put("k", "v")

        assertEquals(listOf("auth", "profile"), factory.storeNames().sorted())
    }
}

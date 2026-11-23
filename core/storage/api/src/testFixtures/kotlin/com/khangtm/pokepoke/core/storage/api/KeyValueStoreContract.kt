package com.khangtm.pokepoke.core.storage.api

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
 * Rules every [KeyValueStoreFactory] must follow. Extend it for the real implementation and for the fake, so both
 * are verified against the same tests. [createFactory] must return a factory whose background work runs in [scope].
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class KeyValueStoreContract {

    abstract fun createFactory(scope: CoroutineScope): KeyValueStoreFactory

    @Test
    fun `missing keys observe null for every type`() = runTest {
        val store = createFactory(backgroundScope).create("contract")
        assertNull(store.observeString("k").first())
        assertNull(store.observeBoolean("k").first())
        assertNull(store.observeLong("k").first())
        assertNull(store.observeInt("k").first())
    }

    @Test
    fun `values round trip for every type`() = runTest {
        val store = createFactory(backgroundScope).create("contract")
        store.putString("s", "pikachu")
        store.putBoolean("b", true)
        store.putLong("l", 9_000_000_000L)
        store.putInt("i", 25)

        assertEquals("pikachu", store.observeString("s").first())
        assertEquals(true, store.observeBoolean("b").first())
        assertEquals(9_000_000_000L, store.observeLong("l").first())
        assertEquals(25, store.observeInt("i").first())
    }

    @Test
    fun `put overwrites the previous value`() = runTest {
        val store = createFactory(backgroundScope).create("contract")
        store.putString("k", "one")
        store.putString("k", "two")
        assertEquals("two", store.observeString("k").first())
    }

    @Test
    fun `keys are independent`() = runTest {
        val store = createFactory(backgroundScope).create("contract")
        store.putString("a", "1")
        store.putString("b", "2")
        store.remove("a")
        assertNull(store.observeString("a").first())
        assertEquals("2", store.observeString("b").first())
    }

    @Test
    fun `writing another type under the same key replaces the value`() = runTest {
        val store = createFactory(backgroundScope).create("contract")
        store.putString("k", "text")
        store.putInt("k", 7)

        assertNull(store.observeString("k").first())
        assertEquals(7, store.observeInt("k").first())
        assertEquals(mapOf<String, Any>("k" to 7), store.observeAll().first())
    }

    @Test
    fun `observing a key as another type emits null`() = runTest {
        val store = createFactory(backgroundScope).create("contract")
        store.putString("k", "text")
        assertNull(store.observeInt("k").first())
        assertNull(store.observeBoolean("k").first())
    }

    @Test
    fun `remove deletes the key and removing a missing key is harmless`() = runTest {
        val store = createFactory(backgroundScope).create("contract")
        store.putInt("k", 1)
        store.remove("k")
        store.remove("never-existed")
        assertNull(store.observeInt("k").first())
        assertTrue(store.observeAll().first().isEmpty())
    }

    @Test
    fun `clear deletes every key`() = runTest {
        val store = createFactory(backgroundScope).create("contract")
        store.putString("a", "1")
        store.putInt("b", 2)
        store.clear()
        assertTrue(store.observeAll().first().isEmpty())
        assertNull(store.observeString("a").first())
    }

    @Test
    fun `observeAll returns every entry with its value`() = runTest {
        val store = createFactory(backgroundScope).create("contract")
        store.putString("s", "x")
        store.putBoolean("b", false)
        store.putLong("l", 3L)
        store.putInt("i", 4)

        assertEquals(
            mapOf<String, Any>("s" to "x", "b" to false, "l" to 3L, "i" to 4),
            store.observeAll().first(),
        )
    }

    @Test
    fun `observe emits later changes`() = runTest {
        val store = createFactory(backgroundScope).create("contract")
        val seen = mutableListOf<String?>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.observeString("k").collect { seen += it }
        }
        advanceUntilIdle()

        store.putString("k", "one")
        advanceUntilIdle()
        store.putString("k", "two")
        advanceUntilIdle()
        store.remove("k")
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

        first.putString("k", "value")
        assertNull(factory.create("two").observeString("k").first())
    }

    @Test
    fun `storeNames lists stores that were written to`() = runTest {
        val factory = createFactory(backgroundScope)
        factory.create("alpha").putString("k", "v")
        factory.create("beta").putInt("k", 1)

        assertEquals(listOf("alpha", "beta"), factory.storeNames().sorted())
    }
}

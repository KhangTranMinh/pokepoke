package com.khangtm.pokepoke.core.storage.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The fake must follow the same contract as the DataStore implementation. */
class FakeKeyValueStoreContractTest : KeyValueStoreContract() {
    override fun createFactory(scope: CoroutineScope): KeyValueStoreFactory = FakeKeyValueStoreFactory()
}

class FakeKeyValueStoreTest {

    @Test
    fun `a store that was never written is not listed`() {
        val factory = FakeKeyValueStoreFactory()
        factory.create("untouched")
        assertTrue(factory.storeNames().isEmpty())
    }

    @Test
    fun `hasBeenWritten turns true on put, remove and clear`() = runTest {
        val factory = FakeKeyValueStoreFactory()
        val store = factory.create("s")
        assertFalse(store.hasBeenWritten)
        store.clear()
        assertTrue(store.hasBeenWritten)
        assertEquals(listOf("s"), factory.storeNames())
    }

    @Test
    fun `factory returns the typed fake`() {
        val factory = FakeKeyValueStoreFactory()
        val store: FakeKeyValueStore = factory.create("typed")
        assertEquals("typed", store.name)
    }

    @Test
    fun `failWith makes every operation throw`() = runTest {
        val store = FakeKeyValueStoreFactory().create("s")
        store.putBoolean("k", true)
        store.failWith = IllegalStateException("disk full")

        listOf<suspend () -> Unit>(
            { store.putString("k", "v") },
            { store.putBoolean("k", true) },
            { store.putLong("k", 1L) },
            { store.putInt("k", 1) },
            { store.remove("k") },
            { store.clear() },
            { store.observeString("k").first() },
            { store.observeBoolean("k").first() },
            { store.observeLong("k").first() },
            { store.observeInt("k").first() },
            { store.observeAll().first() },
        ).forEach { operation ->
            try {
                operation()
                fail("Expected the failure to be thrown")
            } catch (expected: IllegalStateException) {
                assertEquals("disk full", expected.message)
            }
        }

        store.failWith = null
        assertEquals(true, store.observeBoolean("k").first())
    }
}

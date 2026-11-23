package com.khangtm.pokepoke.core.storage.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
}

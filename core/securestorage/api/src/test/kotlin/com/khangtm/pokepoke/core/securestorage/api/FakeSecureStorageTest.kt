package com.khangtm.pokepoke.core.securestorage.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The fake must follow the same contract as the real implementation. */
class FakeSecureStorageContractTest : SecureStorageContract() {
    override fun createFactory(scope: CoroutineScope): SecureStorageFactory = FakeSecureStorageFactory()
}

class FakeSecureStorageTest {

    @Test
    fun `a store that was never written is not listed`() {
        val factory = FakeSecureStorageFactory()
        factory.create("untouched")
        assertTrue(factory.storeNames().isEmpty())
    }

    @Test
    fun `hasBeenWritten turns true on put, remove and clear`() = runTest {
        val storage = FakeSecureStorageFactory().create("s")
        assertFalse(storage.hasBeenWritten)
        storage.clear()
        assertTrue(storage.hasBeenWritten)
    }

    @Test
    fun `failWith makes every operation throw`() = runTest {
        val storage = FakeSecureStorageFactory().create("s")
        storage.put("k", "v")
        storage.failWith = IllegalStateException("disk full")

        listOf<suspend () -> Unit>(
            { storage.get("k") },
            { storage.put("k", "v") },
            { storage.remove("k") },
            { storage.clear() },
            { storage.observe("k").first() },
            { storage.observeKeys().first() },
        ).forEach { operation ->
            try {
                operation()
                fail("Expected the failure to be thrown")
            } catch (expected: IllegalStateException) {
                assertEquals("disk full", expected.message)
            }
        }

        storage.failWith = null
        assertEquals("v", storage.get("k"))
    }
}

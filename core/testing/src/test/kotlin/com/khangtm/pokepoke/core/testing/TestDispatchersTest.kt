package com.khangtm.pokepoke.core.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatchersTest {

    @Test
    fun `all dispatchers share one scheduler`() {
        val dispatchers = TestDispatchers()
        assertSame(dispatchers.scheduler, dispatchers.main.scheduler)
        assertSame(dispatchers.scheduler, dispatchers.io.scheduler)
        assertSame(dispatchers.scheduler, dispatchers.default.scheduler)
    }

    @Test
    fun `dispatchers are distinct instances`() {
        val dispatchers = TestDispatchers()
        assertNotSame(dispatchers.io, dispatchers.default)
        assertNotSame(dispatchers.main, dispatchers.io)
    }

    @Test
    fun `virtual time advances across dispatchers`() = TestDispatchers().let { dispatchers ->
        runTest(dispatchers.main) {
            val events = mutableListOf<String>()
            launch(dispatchers.io) { delay(1_000); events += "io" }
            launch(dispatchers.default) { delay(500); events += "default" }
            testScheduler.advanceTimeBy(1_001)
            testScheduler.runCurrent()
            assertEquals(listOf("default", "io"), events)
            assertEquals(1_001, dispatchers.scheduler.currentTime)
        }
    }
}

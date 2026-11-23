package com.khangtm.pokepoke.core.connectivity

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeNetworkMonitorTest {

    @Test
    fun `is online by default and follows the online property`() {
        val monitor = FakeNetworkMonitor()
        assertTrue(monitor.isOnlineNow())
        monitor.online = false
        assertFalse(monitor.isOnlineNow())
        assertFalse(monitor.online)
    }

    @Test
    fun `isOnline emits the current value then every change`() = runTest {
        val monitor = FakeNetworkMonitor(online = false)
        monitor.isOnline.test {
            assertFalse(awaitItem())
            monitor.online = true
            assertTrue(awaitItem())
            monitor.online = false
            assertFalse(awaitItem())
        }
    }
}

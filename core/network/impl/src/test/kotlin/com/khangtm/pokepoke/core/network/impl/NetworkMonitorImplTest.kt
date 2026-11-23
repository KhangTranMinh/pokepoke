package com.khangtm.pokepoke.core.network.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowConnectivityManager
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(RobolectricTestRunner::class)
class NetworkMonitorImplTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val shadow: ShadowConnectivityManager = shadowOf(connectivityManager)
    private val monitor = NetworkMonitorImpl(context)

    private fun capabilities(internet: Boolean, validated: Boolean): NetworkCapabilities {
        val caps = ShadowNetworkCapabilities.newInstance()
        if (internet) shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (validated) shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return caps
    }

    private fun activate(internet: Boolean, validated: Boolean): Network {
        val network = connectivityManager.activeNetwork ?: error("Robolectric provides a default network")
        shadow.setNetworkCapabilities(network, capabilities(internet, validated))
        return network
    }

    @Test
    fun `isOnlineNow is true with internet and validated`() {
        activate(internet = true, validated = true)
        assertTrue(monitor.isOnlineNow())
    }

    @Test
    fun `isOnlineNow is false without validation`() {
        activate(internet = true, validated = false)
        assertFalse(monitor.isOnlineNow())
    }

    @Test
    fun `isOnlineNow is false without internet capability`() {
        activate(internet = false, validated = true)
        assertFalse(monitor.isOnlineNow())
    }

    @Test
    fun `isOnlineNow is false without an active network`() {
        shadow.setActiveNetworkInfo(null)
        assertFalse(monitor.isOnlineNow())
    }

    @Test
    fun `isOnline emits the current state first`() = runTest {
        activate(internet = true, validated = true)
        monitor.isOnline.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isOnline follows capability changes and loss`() = runTest {
        val network = activate(internet = true, validated = false)
        monitor.isOnline.test {
            assertFalse(awaitItem())

            val callback = shadow.networkCallbacks.single()
            callback.onCapabilitiesChanged(network, capabilities(internet = true, validated = true))
            assertTrue(awaitItem())

            callback.onLost(network)
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isOnline does not repeat equal values`() = runTest {
        val network = activate(internet = true, validated = true)
        monitor.isOnline.test {
            assertTrue(awaitItem())
            val callback = shadow.networkCallbacks.single()
            callback.onCapabilitiesChanged(network, capabilities(internet = true, validated = true))
            callback.onAvailable(network)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `callback is unregistered when collection stops`() = runTest {
        activate(internet = true, validated = true)
        monitor.isOnline.test {
            awaitItem()
            assertEquals(1, shadow.networkCallbacks.size)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(shadow.networkCallbacks.isEmpty())
    }
}

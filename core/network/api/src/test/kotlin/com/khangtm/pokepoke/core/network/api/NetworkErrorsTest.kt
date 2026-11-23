package com.khangtm.pokepoke.core.network.api

import com.khangtm.pokepoke.core.common.DataError
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkErrorsTest {

    @Test
    fun `SocketTimeoutException is Timeout`() {
        assertEquals(DataError.Network.Timeout, SocketTimeoutException("slow").toNetworkDataError())
    }

    @Test
    fun `other IOException is NoConnection`() {
        assertEquals(DataError.Network.NoConnection, IOException("io").toNetworkDataError())
        assertEquals(DataError.Network.NoConnection, UnknownHostException("host").toNetworkDataError())
        assertEquals(DataError.Network.NoConnection, ConnectException("refused").toNetworkDataError())
    }

    @Test
    fun `HTTP 404 is NotFound`() {
        assertEquals(DataError.NotFound, NetworkHttpException(404).toNetworkDataError())
    }

    @Test
    fun `other HTTP codes carry the code and are not treated as a lost connection`() {
        assertEquals(DataError.Network.Http(500), NetworkHttpException(500).toNetworkDataError())
        assertEquals(DataError.Network.Http(401), NetworkHttpException(401).toNetworkDataError())
        assertEquals(DataError.Network.Http(429), NetworkHttpException(429).toNetworkDataError())
    }

    @Test
    fun `NetworkHttpException exposes its code and is an IOException`() {
        val exception = NetworkHttpException(503)
        assertEquals(503, exception.code)
        assertEquals("HTTP 503", exception.message)
        assertTrue(IOException::class.java.isAssignableFrom(exception.javaClass))
    }

    @Test
    fun `SerializationException and IllegalArgumentException are Parse`() {
        val serialization = SerializationException("bad json")
        val illegal = IllegalArgumentException("bad dto")
        assertEquals(DataError.Network.Parse(serialization), serialization.toNetworkDataError())
        assertEquals(DataError.Network.Parse(illegal), illegal.toNetworkDataError())
    }

    @Test
    fun `anything else is Unexpected`() {
        val boom = IllegalStateException("boom")
        assertEquals(DataError.Unexpected(boom), boom.toNetworkDataError())
        assertTrue(RuntimeException().toNetworkDataError() is DataError.Unexpected)
    }
}

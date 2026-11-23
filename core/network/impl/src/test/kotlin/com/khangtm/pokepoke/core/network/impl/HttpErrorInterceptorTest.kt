package com.khangtm.pokepoke.core.network.impl

import com.khangtm.pokepoke.core.network.api.NetworkHttpException
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class HttpErrorInterceptorTest {

    private lateinit var server: MockWebServer
    private val client = OkHttpClient.Builder().addInterceptor(HttpErrorInterceptor).build()

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun execute() = client.newCall(Request.Builder().url(server.url("/")).build()).execute()

    @Test
    fun `2xx responses pass through untouched`() {
        server.enqueue(MockResponse.Builder().code(200).body("ok").build())
        server.enqueue(MockResponse.Builder().code(204).build())

        execute().use { assertEquals("ok", it.body.string()) }
        execute().use { assertEquals(204, it.code) }
    }

    @Test
    fun `non-2xx responses throw NetworkHttpException with the code`() {
        listOf(400, 404, 500, 503).forEach { code ->
            server.enqueue(MockResponse.Builder().code(code).build())
            try {
                execute().use { }
                fail("Expected NetworkHttpException for $code")
            } catch (e: NetworkHttpException) {
                assertEquals(code, e.code)
            }
        }
    }
}

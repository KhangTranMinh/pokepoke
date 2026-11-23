package com.khangtm.pokepoke.core.network

import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import com.khangtm.pokepoke.core.common.dataResultOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Interceptor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.http.GET

@Serializable
private data class FixtureDto(val id: Int)

private interface FixtureApi {
    @GET("fixture")
    suspend fun get(): FixtureDto
}

/** The helper that features use in their Repository tests must behave like production. */
class TestApiServiceFactoryTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun call(factory: ApiServiceFactory): DataResult<FixtureDto> =
        runBlocking { dataResultOf(Throwable::toNetworkDataError) { factory.create<FixtureApi>().get() } }

    @Test
    fun `parses JSON like production including unknown fields`() {
        server.enqueue(MockResponse.Builder().body("""{"id":7,"unknown":true}""").build())
        val result = call(createTestApiServiceFactory(server.url("/").toString()))
        assertEquals(DataResult.Success(FixtureDto(7)), result)
    }

    @Test
    fun `HTTP errors are translated like production`() {
        server.enqueue(MockResponse.Builder().code(404).build())
        val result = call(createTestApiServiceFactory(server.url("/").toString()))
        assertEquals(DataResult.Failure(DataError.NotFound), result)
    }

    @Test
    fun `given interceptors are applied`() {
        server.enqueue(MockResponse.Builder().body("""{"id":1}""").build())
        val header = Interceptor { it.proceed(it.request().newBuilder().header("X-Fixture", "1").build()) }

        call(createTestApiServiceFactory(server.url("/").toString(), setOf(header)))

        assertEquals("1", server.takeRequest().headers["X-Fixture"])
    }
}

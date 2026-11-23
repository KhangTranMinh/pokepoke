package com.khangtm.pokepoke.core.network.impl

import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import com.khangtm.pokepoke.core.common.dataResultOf
import com.khangtm.pokepoke.core.network.api.create
import com.khangtm.pokepoke.core.network.retrofit.toNetworkDataError
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

@Serializable
private data class SampleDto(val id: Int, val name: String, val note: String? = null, val count: Int = 0)

private interface SampleApi {
    @GET("sample")
    suspend fun get(): SampleDto
}

class NetworkFactoriesTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun api(
        interceptors: Set<Interceptor> = emptySet(),
        client: (OkHttpClient) -> OkHttpClient = { it },
    ): SampleApi {
        val okHttp = client(createOkHttpClient(interceptors))
        val retrofit = createRetrofit(server.url("/").toString(), okHttp, createJson())
        return ApiServiceFactoryImpl(retrofit).create()
    }

    private fun json(body: String, code: Int = 200) =
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()

    private fun call(api: SampleApi): DataResult<SampleDto> =
        runBlocking { dataResultOf(Throwable::toNetworkDataError) { api.get() } }

    // ---- JSON ----

    @Test
    fun `service created from the factory parses JSON and ignores unknown fields`() {
        server.enqueue(json("""{"id":25,"name":"pikachu","extra":{"a":1},"another":[1,2]}"""))

        val result = call(api())

        assertEquals(DataResult.Success(SampleDto(id = 25, name = "pikachu")), result)
    }

    @Test
    fun `null for a field with a default is coerced and missing nullable stays null`() {
        server.enqueue(json("""{"id":1,"name":"bulbasaur","count":null}"""))

        val dto = (call(api()) as DataResult.Success).value

        assertEquals(0, dto.count)
        assertNull(dto.note)
    }

    @Test
    fun `request goes to the base url path`() {
        server.enqueue(json("""{"id":1,"name":"a"}"""))
        call(api())
        assertEquals("/sample", server.takeRequest().url.encodedPath)
    }

    // ---- Interceptors / client ----

    @Test
    fun `interceptors in the set are called`() {
        server.enqueue(json("""{"id":1,"name":"a"}"""))
        var calls = 0
        val interceptor = Interceptor { chain ->
            calls++
            chain.proceed(chain.request().newBuilder().header("X-Test", "yes").build())
        }

        call(api(setOf(interceptor)))

        assertEquals(1, calls)
        assertEquals("yes", server.takeRequest().headers["X-Test"])
    }

    @Test
    fun `every interceptor in the set is applied`() {
        server.enqueue(json("""{"id":1,"name":"a"}"""))
        val first = Interceptor { it.proceed(it.request().newBuilder().header("X-A", "1").build()) }
        val second = Interceptor { it.proceed(it.request().newBuilder().header("X-B", "2").build()) }

        call(api(setOf(first, second)))

        val request = server.takeRequest()
        assertEquals("1", request.headers["X-A"])
        assertEquals("2", request.headers["X-B"])
    }

    @Test
    fun `client uses the documented timeouts`() {
        val client = createOkHttpClient(emptySet())
        assertEquals(10_000, client.connectTimeoutMillis)
        assertEquals(15_000, client.readTimeoutMillis)
        assertTrue(client.interceptors.isEmpty())
    }

    @Test
    fun `json is configured to be lenient with the API`() {
        val json = createJson()
        assertTrue(json.configuration.ignoreUnknownKeys)
        assertTrue(json.configuration.coerceInputValues)
        assertEquals(false, json.configuration.explicitNulls)
    }

    // ---- Error translation end to end ----

    @Test
    fun `HTTP 404 becomes NotFound`() {
        server.enqueue(json("{}", code = 404))
        assertEquals(DataResult.Failure(DataError.NotFound), call(api()))
    }

    @Test
    fun `HTTP 500 becomes Http 500`() {
        server.enqueue(json("{}", code = 500))
        assertEquals(DataResult.Failure(DataError.Network.Http(500)), call(api()))
    }

    @Test
    fun `malformed JSON becomes Parse`() {
        server.enqueue(json("this is not json"))
        val error = (call(api()) as DataResult.Failure).error
        assertTrue(error is DataError.Network.Parse)
    }

    @Test
    fun `JSON missing a required field becomes Parse`() {
        server.enqueue(json("""{"name":"no id"}"""))
        val error = (call(api()) as DataResult.Failure).error
        assertTrue(error is DataError.Network.Parse)
    }

    @Test
    fun `slow server becomes Timeout`() {
        server.enqueue(
            MockResponse.Builder().code(200).body("{}").headersDelay(2, TimeUnit.SECONDS).build(),
        )
        val api = api(client = { it.newBuilder().readTimeout(200, TimeUnit.MILLISECONDS).build() })
        assertEquals(DataResult.Failure(DataError.Network.Timeout), call(api))
    }

    @Test
    fun `connection dropped becomes NoConnection`() {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())
        assertEquals(DataResult.Failure(DataError.Network.NoConnection), call(api()))
    }

    @Test
    fun `server not reachable becomes NoConnection`() {
        val api = api()
        server.close()
        assertEquals(DataResult.Failure(DataError.Network.NoConnection), call(api))
    }
}

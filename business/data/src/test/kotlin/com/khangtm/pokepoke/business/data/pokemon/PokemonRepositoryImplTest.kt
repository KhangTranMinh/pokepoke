package com.khangtm.pokepoke.business.data.pokemon

import com.khangtm.pokepoke.business.data.pokemon.db.PokemonDatabase
import com.khangtm.pokepoke.business.domain.pokemon.PokemonFixtures
import com.khangtm.pokepoke.business.domain.pokemon.PokemonRepository
import com.khangtm.pokepoke.business.domain.pokemon.PokemonSummary
import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import com.khangtm.pokepoke.core.connectivity.FakeNetworkMonitor
import com.khangtm.pokepoke.core.network.create
import com.khangtm.pokepoke.core.network.createTestApiServiceFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.Interceptor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.SocketTimeoutException

/** Serves canned responses by path; unknown paths are 404. */
private class RouteDispatcher : Dispatcher() {
    private val routes = mutableMapOf<String, Pair<Int, String>>()
    val paths = mutableListOf<String>()

    fun json(path: String, resource: String) {
        routes[path] = 200 to PokemonJsonSamples.text(resource)
    }

    fun respond(path: String, code: Int, body: String = "") {
        routes[path] = code to body
    }

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.url.encodedPath
        paths += path
        val (code, body) = routes[path] ?: (404 to "")
        return MockResponse.Builder().code(code).body(body).build()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PokemonRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var routes: RouteDispatcher
    private lateinit var database: PokemonDatabase
    private val monitor = FakeNetworkMonitor(online = true)
    private var interceptors: Set<Interceptor> = emptySet()

    private val success = DataResult.Success(Unit)

    @Before
    fun setUp() {
        routes = RouteDispatcher()
        server = MockWebServer().apply {
            dispatcher = routes
            start()
        }
        database = createInMemoryDatabase()
    }

    @After
    fun tearDown() {
        database.close()
        server.close()
    }

    private fun repository(): PokemonRepositoryImpl {
        val factory = createTestApiServiceFactory(server.url("/").toString(), interceptors)
        return PokemonRepositoryImpl(factory.create<PokemonApi>(), database.pokemonDao(), monitor, UnconfinedTestDispatcher())
    }

    private fun <T> DataResult<T>.value(): T = when (this) {
        is DataResult.Success -> value
        is DataResult.Failure -> throw AssertionError("Expected Success but was $error")
    }

    private fun failure(error: DataError) = DataResult.Failure(error)

    private fun serveDetailChain() {
        routes.json("/pokemon/1", "pokemon_1.json")
        routes.json("/pokemon-species/1", "species_1.json")
        routes.json("/evolution-chain/1", "evolution_chain_1.json")
    }

    private suspend fun assertNothingStored(repository: PokemonRepository) {
        assertEquals(emptyList<PokemonSummary>(), repository.observeList(100).first().value())
        assertEquals(null, repository.observeTotalCount().first().value())
        assertEquals(failure(DataError.NotFound), repository.observeDetail(1).first())
        assertEquals(failure(DataError.NotFound), repository.observeByNameOrId("1").first())
    }

    // ---- refreshPage

    @Test
    fun `refreshPage saves the page with positions and the total count`() = runTest {
        routes.json("/pokemon", "pokemon_list.json")
        val repository = repository()

        assertEquals(success, repository.refreshPage(offset = 0, limit = 3))

        assertEquals(
            listOf(PokemonFixtures.bulbasaur, PokemonFixtures.ivysaur, PokemonFixtures.venusaur),
            repository.observeList(10).first().value(),
        )
        assertEquals(1302, repository.observeTotalCount().first().value())
        val request = server.takeRequest()
        assertEquals("offset=0&limit=3", request.url.query)
    }

    @Test
    fun `refreshPage at an offset assigns positions from the offset`() = runTest {
        routes.json("/pokemon", "pokemon_list.json")
        val repository = repository()
        repository.refreshPage(offset = 3, limit = 3)
        assertEquals(listOf(1, 2, 3), repository.observeList(100).first().value().map { it.id })
        assertEquals(3, repository.observeList(100).first().value().size)
        assertEquals("offset=3&limit=3", server.takeRequest().url.query)
    }

    @Test
    fun `refreshPage keeps a stored detail and skips items without a readable id`() = runTest {
        val dao = database.pokemonDao()
        dao.saveDetail(
            PokemonFixtures.bulbasaur.toEntity(),
            PokemonFixtures.bulbasaurDetail.toEntity(PokemonJsonCodec(), updatedAt = 1),
        )
        routes.json("/pokemon", "pokemon_list_unparseable.json")
        val repository = repository()

        assertEquals(success, repository.refreshPage(0, 3))

        assertEquals(
            listOf(PokemonFixtures.bulbasaur, PokemonFixtures.venusaur),
            repository.observeList(10).first().value(),
        )
        assertEquals(PokemonFixtures.bulbasaurDetail, repository.observeDetail(1).first().value())
    }

    // ---- refreshByNameOrId

    @Test
    fun `refreshByNameOrId saves the summary without a list position`() = runTest {
        routes.json("/pokemon/pikachu", "pokemon_25.json")
        val repository = repository()

        assertEquals(success, repository.refreshByNameOrId("pikachu"))

        assertEquals(PokemonFixtures.pikachu, repository.observeByNameOrId("pikachu").first().value())
        assertEquals(emptyList<PokemonSummary>(), repository.observeList(10).first().value())
    }

    @Test
    fun `refreshByNameOrId keeps the list position and the detail`() = runTest {
        val dao = database.pokemonDao()
        dao.saveList(listOf(PokemonFixtures.pikachu.toEntity(), PokemonFixtures.raichu.toEntity()), 0, 2)
        dao.saveDetail(
            PokemonFixtures.pikachu.toEntity(),
            PokemonFixtures.pikachuDetail.toEntity(PokemonJsonCodec(), updatedAt = 1),
        )
        routes.json("/pokemon/25", "pokemon_25.json")
        val repository = repository()

        assertEquals(success, repository.refreshByNameOrId("25"))

        assertEquals(listOf(PokemonFixtures.pikachu, PokemonFixtures.raichu), repository.observeList(10).first().value())
        assertEquals(PokemonFixtures.pikachuDetail, repository.observeDetail(25).first().value())
    }

    @Test
    fun `refreshByNameOrId with an unknown name is NotFound and stores nothing`() = runTest {
        val repository = repository()
        assertEquals(failure(DataError.NotFound), repository.refreshByNameOrId("missingno"))
        assertNothingStored(repository)
    }

    // ---- refreshDetail

    @Test
    fun `refreshDetail calls pokemon then species then evolution chain and saves the detail`() = runTest {
        serveDetailChain()
        val repository = repository()

        assertEquals(success, repository.refreshDetail(1))

        assertEquals(listOf("/pokemon/1", "/pokemon-species/1", "/evolution-chain/1"), routes.paths)
        assertEquals(PokemonFixtures.bulbasaurDetail, repository.observeDetail(1).first().value())
    }

    @Test
    fun `refreshDetail does not change the list position and does not add the pokemon to the list`() = runTest {
        serveDetailChain()
        val repository = repository()
        database.pokemonDao().saveList(listOf(PokemonFixtures.bulbasaur, PokemonFixtures.ivysaur).map { it.toEntity() }, 0, 2)

        repository.refreshDetail(1)

        assertEquals(listOf(PokemonFixtures.bulbasaur, PokemonFixtures.ivysaur), repository.observeList(10).first().value())
    }

    @Test
    fun `refreshDetail of a pokemon outside the list does not give it a list position`() = runTest {
        routes.json("/pokemon/25", "pokemon_25.json")
        routes.json("/pokemon-species/25", "species_25.json")
        routes.json("/evolution-chain/10", "evolution_chain_10.json")
        val repository = repository()

        assertEquals(success, repository.refreshDetail(25))

        assertEquals(emptyList<PokemonSummary>(), repository.observeList(10).first().value())
        val detail = repository.observeDetail(25).first().value()
        assertEquals(PokemonFixtures.pikachu, detail.summary)
        assertEquals(listOf("pichu"), listOf(detail.evolutionChain.pokemon.name))
    }

    @Test
    fun `refreshDetail twice replaces the previous detail`() = runTest {
        serveDetailChain()
        val repository = repository()
        repository.refreshDetail(1)
        routes.json("/evolution-chain/1", "evolution_chain_single.json")
        assertEquals(success, repository.refreshDetail(1))
        val detail = repository.observeDetail(1).first().value()
        assertEquals(emptyList<Any>(), detail.evolutionChain.evolvesTo)
        assertEquals("ditto", detail.evolutionChain.pokemon.name)
    }

    @Test
    fun `an error at each step of the three call chain leaves the storage unchanged`() = runTest {
        val steps = listOf(
            "/pokemon/1" to 3,
            "/pokemon-species/1" to 2,
            "/evolution-chain/1" to 1,
        )
        for ((failingPath, _) in steps) {
            serveDetailChain()
            routes.respond(failingPath, 500)
            routes.paths.clear()
            val repository = repository()

            assertEquals("step $failingPath", failure(DataError.Network.Http(500)), repository.refreshDetail(1))

            assertNothingStored(repository)
            // The chain stops at the failing step.
            assertEquals(failingPath, routes.paths.last())
        }
    }

    @Test
    fun `an unknown pokemon id is NotFound`() = runTest {
        val repository = repository()
        assertEquals(failure(DataError.NotFound), repository.refreshDetail(99999))
        assertNothingStored(repository)
    }

    @Test
    fun `a missing species or chain is NotFound`() = runTest {
        routes.json("/pokemon/1", "pokemon_1.json")
        val repository = repository()
        assertEquals(failure(DataError.NotFound), repository.refreshDetail(1))
        routes.json("/pokemon-species/1", "species_1.json")
        assertEquals(failure(DataError.NotFound), repository.refreshDetail(1))
        assertNothingStored(repository)
    }

    @Test
    fun `a species url without an id is a parse error`() = runTest {
        routes.json("/pokemon/1", "pokemon_bad_species_url.json")
        val result = repository().refreshDetail(1)
        assertTrue(result is DataResult.Failure && result.error is DataError.Network.Parse)
    }

    @Test
    fun `an unreadable evolution chain id is a parse error`() = runTest {
        routes.json("/pokemon/1", "pokemon_1.json")
        routes.json("/pokemon-species/1", "species_1.json")
        routes.json("/evolution-chain/1", "evolution_chain_bad_root.json")
        val result = repository().refreshDetail(1)
        assertTrue(result is DataResult.Failure && result.error is DataError.Network.Parse)
    }

    // ---- offline

    @Test
    fun `offline every refresh is NoConnection and the API is not called`() = runTest {
        monitor.online = false
        val repository = repository()
        val offline = failure(DataError.Network.NoConnection)

        assertEquals(offline, repository.refreshPage(0, 20))
        assertEquals(offline, repository.refreshByNameOrId("pikachu"))
        assertEquals(offline, repository.refreshDetail(1))

        assertEquals(0, server.requestCount)
        assertNothingStored(repository)
    }

    @Test
    fun `coming back online refreshes normally`() = runTest {
        routes.json("/pokemon", "pokemon_list.json")
        monitor.online = false
        val repository = repository()
        repository.refreshPage(0, 3)
        monitor.online = true
        assertEquals(success, repository.refreshPage(0, 3))
        assertEquals(1, server.requestCount)
    }

    // ---- error translation (layers.md §2)

    @Test
    fun `HTTP 404 is NotFound and 500 is Http`() = runTest {
        routes.respond("/pokemon", 500)
        val repository = repository()
        assertEquals(failure(DataError.Network.Http(500)), repository.refreshPage(0, 3))
        routes.respond("/pokemon", 404)
        assertEquals(failure(DataError.NotFound), repository.refreshPage(0, 3))
        assertNothingStored(repository)
    }

    @Test
    fun `a timeout is Timeout`() = runTest {
        interceptors = setOf(Interceptor { throw SocketTimeoutException("slow") })
        val repository = repository()
        assertEquals(failure(DataError.Network.Timeout), repository.refreshPage(0, 3))
        assertEquals(failure(DataError.Network.Timeout), repository.refreshByNameOrId("pikachu"))
        assertEquals(failure(DataError.Network.Timeout), repository.refreshDetail(1))
        assertNothingStored(repository)
    }

    @Test
    fun `a refused connection is NoConnection`() = runTest {
        val repository = repository()
        server.close()
        assertEquals(failure(DataError.Network.NoConnection), repository.refreshPage(0, 3))
        assertNothingStored(repository)
    }

    @Test
    fun `broken json is a parse error and stores nothing`() = runTest {
        routes.respond("/pokemon", 200, "{ not json")
        routes.respond("/pokemon/pikachu", 200, """{"id": "abc"}""")
        val repository = repository()

        val page = repository.refreshPage(0, 3)
        val byName = repository.refreshByNameOrId("pikachu")

        assertTrue(page is DataResult.Failure && page.error is DataError.Network.Parse)
        assertTrue(byName is DataResult.Failure && byName.error is DataError.Network.Parse)
        assertNothingStored(repository)
    }

    @Test
    fun `a blank query is a parse error not a crash`() = runTest {
        val result = repository().refreshByNameOrId("")
        assertTrue(result is DataResult.Failure)
    }

    @Test
    fun `a write failure is Local Write`() = runTest {
        routes.json("/pokemon", "pokemon_list.json")
        routes.json("/pokemon/pikachu", "pokemon_25.json")
        serveDetailChain()
        val repository = repository()
        database.close()

        listOf(
            repository.refreshPage(0, 3),
            repository.refreshByNameOrId("pikachu"),
            repository.refreshDetail(1),
        ).forEach { result ->
            assertTrue("Expected Local.Write but was $result", result is DataResult.Failure && result.error is DataError.Local.Write)
        }
    }

    // ---- local reads

    @Test
    fun `every observe function reports Local Read when the database fails`() = runTest {
        val repository = repository()
        database.close()
        listOf(
            repository.observeList(10).first(),
            repository.observeTotalCount().first(),
            repository.observeDetail(1).first(),
            repository.observeByNameOrId("1").first(),
            repository.observeByNameOrId("bulbasaur").first(),
            repository.observeSummaries(listOf(1, 2)).first(),
        ).forEach { result ->
            assertTrue("Expected Local.Read but was $result", result is DataResult.Failure && result.error is DataError.Local.Read)
        }
    }

    @Test
    fun `a detail row with broken json reports Local Read`() = runTest {
        val codec = PokemonJsonCodec()
        val good = PokemonFixtures.bulbasaurDetail.toEntity(codec, updatedAt = 0)
        database.pokemonDao().saveDetail(PokemonFixtures.bulbasaur.toEntity(), good.copy(typesJson = "not json"))

        val result = repository().observeDetail(1).first()

        assertTrue(result is DataResult.Failure && result.error is DataError.Local.Read)
    }

    @Test
    fun `detail is saved with the current time`() = runTest {
        serveDetailChain()
        assertEquals(success, repository().refreshDetail(1))
        val updatedAt = database.openHelper.readableDatabase.query("SELECT updated_at FROM pokemon_detail").use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }
        assertTrue(updatedAt > 0)
    }
}

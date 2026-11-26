package com.khangtm.pokepoke.business.data.pokemon

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.khangtm.pokepoke.business.data.pokemon.db.PokemonDao
import com.khangtm.pokepoke.business.data.pokemon.db.PokemonDatabase
import com.khangtm.pokepoke.business.domain.pokemon.PokemonDetail
import com.khangtm.pokepoke.business.domain.pokemon.PokemonFixtures
import com.khangtm.pokepoke.business.domain.pokemon.PokemonRepository
import com.khangtm.pokepoke.business.domain.pokemon.PokemonSummary
import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import com.khangtm.pokepoke.core.connectivity.FakeNetworkMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
internal fun createInMemoryDatabase(): PokemonDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), PokemonDatabase::class.java)
        .allowMainThreadQueries()
        .build()

/** Reads through the repository, writes through the DAO: the merge rules of T13 live in the DAO. */
private class RoomStore(private val dao: PokemonDao, repository: PokemonRepository) : PokemonRepository by repository {
    suspend fun saveList(items: List<PokemonSummary>, offset: Int, totalCount: Int): DataResult<Unit> {
        dao.saveList(items.map { it.toEntity() }, offset, totalCount)
        return DataResult.Success(Unit)
    }

    suspend fun saveSummary(summary: PokemonSummary): DataResult<Unit> {
        dao.saveSummary(summary.id, summary.name, summary.imageUrl)
        return DataResult.Success(Unit)
    }

    suspend fun saveDetail(detail: PokemonDetail): DataResult<Unit> {
        dao.saveDetail(detail.summary.toEntity(), detail.toEntity(PokemonJsonCodec(), updatedAt = 1))
        return DataResult.Success(Unit)
    }
}

/** Merge rules of the Room storage (list position, summary, detail), checked through the DAO and the repository. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PokemonDaoTest {

    private lateinit var database: PokemonDatabase

    @Before
    fun openDatabase() {
        database = createInMemoryDatabase()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun createRepository(): RoomStore {
        val dao = database.pokemonDao()
        val repository = PokemonRepositoryImpl(
            api = FailingPokemonApi,
            dao = dao,
            networkMonitor = FakeNetworkMonitor(online = false),
            ioDispatcher = UnconfinedTestDispatcher(),
        )
        return RoomStore(dao, repository)
    }

    private val a = PokemonFixtures.bulbasaur
    private val b = PokemonFixtures.ivysaur
    private val c = PokemonFixtures.venusaur

    private fun <T> DataResult<T>.value(): T = when (this) {
        is DataResult.Success -> value
        is DataResult.Failure -> throw AssertionError("Expected Success but was $error")
    }

    private val success = DataResult.Success(Unit)

    @Test
    fun `nothing stored yet gives an empty list and a null total`() = runTest {
        val repository = createRepository()
        assertEquals(DataResult.Success(emptyList<PokemonSummary>()), repository.observeList(10).first())
        assertEquals(DataResult.Success(null), repository.observeTotalCount().first())
    }

    @Test
    fun `saveList stores items ordered by position and the total count`() = runTest {
        val repository = createRepository()
        assertEquals(success, repository.saveList(listOf(a, b, c), offset = 0, totalCount = 1302))
        assertEquals(listOf(a, b, c), repository.observeList(10).first().value())
        assertEquals(1302, repository.observeTotalCount().first().value())
    }

    @Test
    fun `observeList respects the limit and the position order`() = runTest {
        val repository = createRepository()
        repository.saveList(listOf(b, c), offset = 1, totalCount = 3)
        repository.saveList(listOf(a), offset = 0, totalCount = 3)
        assertEquals(listOf(a, b), repository.observeList(2).first().value())
        assertEquals(listOf(a, b, c), repository.observeList(10).first().value())
        assertEquals(emptyList<PokemonSummary>(), repository.observeList(0).first().value())
    }

    @Test
    fun `saveList positions follow offset plus index not the id`() = runTest {
        val repository = createRepository()
        val formLast = PokemonFixtures.summary(10001, "deoxys-attack")
        repository.saveList(listOf(formLast), offset = 5, totalCount = 6)
        repository.saveList(listOf(c, a), offset = 3, totalCount = 6)
        assertEquals(listOf(c, a, formLast), repository.observeList(10).first().value())
    }

    @Test
    fun `a duplicate position is replaced by the newer item`() = runTest {
        val repository = createRepository()
        repository.saveList(listOf(a, b), offset = 0, totalCount = 3)
        repository.saveList(listOf(c), offset = 1, totalCount = 3)
        assertEquals(listOf(a, c), repository.observeList(10).first().value())
        // The replaced pokemon still exists, only without a list position.
        assertEquals(b, repository.observeSummaries(listOf(b.id)).first().value().single())
    }

    @Test
    fun `saveList updates name and image of an existing pokemon`() = runTest {
        val repository = createRepository()
        repository.saveList(listOf(a), offset = 0, totalCount = 1)
        val renamed = a.copy(name = "bulba", imageUrl = "https://example.com/1.png")
        repository.saveList(listOf(renamed), offset = 0, totalCount = 1)
        assertEquals(listOf(renamed), repository.observeList(10).first().value())
    }

    @Test
    fun `saveList does not delete detail`() = runTest {
        val repository = createRepository()
        repository.saveDetail(PokemonFixtures.bulbasaurDetail)
        repository.saveList(listOf(a, b), offset = 0, totalCount = 2)
        assertEquals(PokemonFixtures.bulbasaurDetail, repository.observeDetail(a.id).first().value())
    }

    @Test
    fun `saveDetail does not lose the list position`() = runTest {
        val repository = createRepository()
        repository.saveList(listOf(a, b, c), offset = 0, totalCount = 3)
        assertEquals(success, repository.saveDetail(PokemonFixtures.ivysaurDetail))
        assertEquals(listOf(a, b, c), repository.observeList(10).first().value())
    }

    @Test
    fun `saveDetail of a pokemon outside the list does not add it to the list`() = runTest {
        val repository = createRepository()
        repository.saveDetail(PokemonFixtures.pikachuDetail)
        assertEquals(emptyList<PokemonSummary>(), repository.observeList(10).first().value())
        assertEquals(PokemonFixtures.pikachu, repository.observeByNameOrId("25").first().value())
    }

    @Test
    fun `saveSummary keeps the list position and the detail`() = runTest {
        val repository = createRepository()
        repository.saveList(listOf(a, b), offset = 0, totalCount = 2)
        repository.saveDetail(PokemonFixtures.bulbasaurDetail)
        val renamed = a.copy(name = "bulba")
        assertEquals(success, repository.saveSummary(renamed))
        assertEquals(listOf(renamed, b), repository.observeList(10).first().value())
        assertEquals(
            PokemonFixtures.bulbasaurDetail.copy(summary = renamed),
            repository.observeDetail(a.id).first().value(),
        )
    }

    @Test
    fun `saveSummary of a new pokemon does not give it a list position`() = runTest {
        val repository = createRepository()
        repository.saveSummary(a)
        assertEquals(emptyList<PokemonSummary>(), repository.observeList(10).first().value())
        assertEquals(a, repository.observeByNameOrId("bulbasaur").first().value())
    }

    @Test
    fun `detail round trips including nullable fields and the evolution tree`() = runTest {
        val repository = createRepository()
        val details = listOf(
            PokemonFixtures.bulbasaurDetail,
            PokemonFixtures.pikachuDetail,
            PokemonFixtures.eeveeDetail,
        )
        details.forEach { assertEquals(success, repository.saveDetail(it)) }
        details.forEach { assertEquals(it, repository.observeDetail(it.summary.id).first().value()) }
        assertEquals(8, repository.observeDetail(133).first().value().evolutionChain.evolvesTo.size)
    }

    @Test
    fun `saveDetail twice replaces the previous detail`() = runTest {
        val repository = createRepository()
        repository.saveDetail(PokemonFixtures.bulbasaurDetail)
        val updated = PokemonFixtures.bulbasaurDetail.copy(heightDecimeters = 99, baseExperience = null)
        repository.saveDetail(updated)
        assertEquals(updated, repository.observeDetail(a.id).first().value())
    }

    @Test
    fun `observing a missing detail fails with NotFound`() = runTest {
        val repository = createRepository()
        assertEquals(DataResult.Failure(DataError.NotFound), repository.observeDetail(999).first())
    }

    @Test
    fun `a pokemon with only a summary has no detail`() = runTest {
        val repository = createRepository()
        repository.saveList(listOf(a), offset = 0, totalCount = 1)
        assertEquals(DataResult.Failure(DataError.NotFound), repository.observeDetail(a.id).first())
    }

    @Test
    fun `observeByNameOrId finds by number`() = runTest {
        val repository = createRepository()
        repository.saveList(listOf(a, b), offset = 0, totalCount = 2)
        assertEquals(b, repository.observeByNameOrId("2").first().value())
        assertEquals(b, repository.observeByNameOrId(" 002 ").first().value())
    }

    @Test
    fun `observeByNameOrId finds by name ignoring case and spaces`() = runTest {
        val repository = createRepository()
        repository.saveList(listOf(a, b), offset = 0, totalCount = 2)
        assertEquals(b, repository.observeByNameOrId("  IvySaur ").first().value())
    }

    @Test
    fun `observeByNameOrId finds hyphenated names`() = runTest {
        val repository = createRepository()
        repository.saveSummary(PokemonFixtures.mrMime)
        assertEquals(PokemonFixtures.mrMime, repository.observeByNameOrId("mr-mime").first().value())
    }

    @Test
    fun `observeByNameOrId with an unknown name or number fails with NotFound`() = runTest {
        val repository = createRepository()
        repository.saveList(listOf(a), offset = 0, totalCount = 1)
        val notFound = DataResult.Failure(DataError.NotFound)
        assertEquals(notFound, repository.observeByNameOrId("missingno").first())
        assertEquals(notFound, repository.observeByNameOrId("999").first())
        assertEquals(notFound, repository.observeByNameOrId("99999999999999999999").first())
        assertEquals(notFound, repository.observeByNameOrId("   ").first())
    }

    @Test
    fun `observeSummaries keeps the requested order and skips missing ids`() = runTest {
        val repository = createRepository()
        repository.saveList(listOf(a, b, c), offset = 0, totalCount = 3)
        assertEquals(listOf(c, a, b), repository.observeSummaries(listOf(3, 1, 2)).first().value())
        assertEquals(listOf(c, a), repository.observeSummaries(listOf(3, 404, 1)).first().value())
        assertEquals(emptyList<PokemonSummary>(), repository.observeSummaries(emptyList()).first().value())
    }

    @Test
    fun `observers see later writes`() = runTest {
        val repository = createRepository()
        val flow = repository.observeList(10)
        assertEquals(emptyList<PokemonSummary>(), flow.first().value())
        repository.saveList(listOf(a), offset = 0, totalCount = 1)
        assertEquals(listOf(a), flow.first().value())
    }
}

/** The DAO tests never reach the network. */
internal object FailingPokemonApi : PokemonApi {
    override suspend fun list(offset: Int, limit: Int): PokemonListDto = error("not used")
    override suspend fun pokemonBasic(idOrName: String): PokemonBasicDto = error("not used")
    override suspend fun pokemonDetail(id: Int): PokemonDto = error("not used")
    override suspend fun species(id: Int): SpeciesDto = error("not used")
    override suspend fun evolutionChain(id: Int): EvolutionChainDto = error("not used")
}

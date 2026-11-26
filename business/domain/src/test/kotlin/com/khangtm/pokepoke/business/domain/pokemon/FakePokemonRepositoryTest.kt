package com.khangtm.pokepoke.business.domain.pokemon

import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakePokemonRepositoryTest {

    private val error = DataError.Local.Read(IllegalStateException("boom"))
    private val success = DataResult.Success(Unit)
    private val a = PokemonFixtures.bulbasaur
    private val b = PokemonFixtures.ivysaur
    private val c = PokemonFixtures.venusaur

    private fun <T> DataResult<T>.value(): T = when (this) {
        is DataResult.Success -> value
        is DataResult.Failure -> throw AssertionError("Expected Success but was $error")
    }

    private fun fakeWithServer() = FakePokemonRepository().apply {
        serverList = listOf(a, b, c)
        serverDetails = mapOf(
            1 to PokemonFixtures.bulbasaurDetail,
            25 to PokemonFixtures.pikachuDetail,
        )
    }

    @Test
    fun `nothing stored gives an empty list and a null total`() = runTest {
        val repository = FakePokemonRepository()
        assertEquals(DataResult.Success(emptyList<PokemonSummary>()), repository.observeList(10).first())
        assertEquals(DataResult.Success(null), repository.observeTotalCount().first())
    }

    @Test
    fun `refreshPage copies a page of the server list with positions and the total`() = runTest {
        val repository = fakeWithServer()
        assertEquals(success, repository.refreshPage(offset = 1, limit = 2))
        assertEquals(listOf(b, c), repository.observeList(10).first().value())
        assertEquals(3, repository.observeTotalCount().first().value())
        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun `refreshPage beyond the end saves nothing but the total`() = runTest {
        val repository = fakeWithServer()
        assertEquals(success, repository.refreshPage(offset = 20, limit = 20))
        assertEquals(emptyList<PokemonSummary>(), repository.observeList(10).first().value())
        assertEquals(3, repository.observeTotalCount().first().value())
    }

    @Test
    fun `list respects the limit and the position order`() = runTest {
        val repository = FakePokemonRepository()
        repository.seedList(listOf(b, c), offset = 1, totalCount = 3)
        repository.seedList(listOf(a), offset = 0, totalCount = 3)
        assertEquals(listOf(a, b), repository.observeList(2).first().value())
        assertEquals(listOf(a, b, c), repository.observeList(10).first().value())
    }

    @Test
    fun `a duplicate position is replaced by the newer item but the old one stays stored`() = runTest {
        val repository = FakePokemonRepository()
        repository.seedList(listOf(a, b), offset = 0, totalCount = 3)
        repository.seedList(listOf(c), offset = 1, totalCount = 3)
        assertEquals(listOf(a, c), repository.observeList(10).first().value())
        assertEquals(listOf(b), repository.observeSummaries(listOf(b.id)).first().value())
    }

    @Test
    fun `refreshDetail stores summary and detail and keeps the list position`() = runTest {
        val repository = fakeWithServer()
        repository.seedList(listOf(a, b), offset = 0, totalCount = 3)
        assertEquals(success, repository.refreshDetail(1))
        assertEquals(PokemonFixtures.bulbasaurDetail, repository.observeDetail(1).first().value())
        assertEquals(listOf(a, b), repository.observeList(10).first().value())
    }

    @Test
    fun `refreshDetail of a pokemon outside the list does not add it to the list`() = runTest {
        val repository = fakeWithServer()
        repository.refreshDetail(25)
        assertEquals(emptyList<PokemonSummary>(), repository.observeList(10).first().value())
        assertEquals(PokemonFixtures.pikachu, repository.observeByNameOrId("25").first().value())
    }

    @Test
    fun `refreshPage does not delete a stored detail`() = runTest {
        val repository = fakeWithServer()
        repository.seedDetail(PokemonFixtures.bulbasaurDetail)
        repository.refreshPage(0, 20)
        assertEquals(PokemonFixtures.bulbasaurDetail, repository.observeDetail(1).first().value())
    }

    @Test
    fun `refreshByNameOrId stores the summary without a list position`() = runTest {
        val repository = fakeWithServer()
        assertEquals(success, repository.refreshByNameOrId("pikachu"))
        assertEquals(PokemonFixtures.pikachu, repository.observeByNameOrId("pikachu").first().value())
        assertEquals(emptyList<PokemonSummary>(), repository.observeList(10).first().value())
        assertEquals(success, repository.refreshByNameOrId(" 2 "))
        assertEquals(b, repository.observeByNameOrId("2").first().value())
    }

    @Test
    fun `refreshByNameOrId keeps the list position and the detail`() = runTest {
        val repository = fakeWithServer()
        repository.seedList(listOf(a), offset = 0, totalCount = 3)
        repository.seedDetail(PokemonFixtures.bulbasaurDetail)
        repository.refreshByNameOrId("bulbasaur")
        assertEquals(listOf(a), repository.observeList(10).first().value())
        assertEquals(PokemonFixtures.bulbasaurDetail, repository.observeDetail(1).first().value())
    }

    @Test
    fun `refresh of something missing on the server fails with NotFound and stores nothing`() = runTest {
        val repository = fakeWithServer()
        val notFound = DataResult.Failure(DataError.NotFound)
        assertEquals(notFound, repository.refreshByNameOrId("missingno"))
        assertEquals(notFound, repository.refreshByNameOrId("999"))
        assertEquals(notFound, repository.refreshByNameOrId("99999999999999999999"))
        assertEquals(notFound, repository.refreshByNameOrId("  "))
        assertEquals(notFound, repository.refreshDetail(404))
        assertEquals(notFound, repository.observeByNameOrId("missingno").first())
        assertEquals(5, repository.refreshCalls)
    }

    @Test
    fun `refreshError fails every refresh and leaves the storage unchanged`() = runTest {
        val repository = fakeWithServer()
        repository.seedList(listOf(a), offset = 0, totalCount = 3)
        repository.refreshError = DataError.Network.NoConnection
        val failure = DataResult.Failure(DataError.Network.NoConnection)

        assertEquals(failure, repository.refreshPage(0, 20))
        assertEquals(failure, repository.refreshByNameOrId("pikachu"))
        assertEquals(failure, repository.refreshDetail(1))

        assertEquals(3, repository.refreshCalls)
        assertEquals(listOf(a), repository.observeList(10).first().value())
        assertEquals(DataResult.Failure(DataError.NotFound), repository.observeDetail(1).first())
        // Observing still works.
        repository.refreshError = null
        assertEquals(success, repository.refreshPage(0, 20))
    }

    @Test
    fun `failWith makes every function fail and clearing it recovers`() = runTest {
        val repository = fakeWithServer()
        repository.failWith = error
        val failure = DataResult.Failure(error)

        assertEquals(failure, repository.observeList(10).first())
        assertEquals(failure, repository.observeTotalCount().first())
        assertEquals(failure, repository.observeDetail(1).first())
        assertEquals(failure, repository.observeByNameOrId("1").first())
        assertEquals(failure, repository.observeSummaries(listOf(1)).first())
        assertEquals(failure, repository.refreshPage(0, 20))
        assertEquals(failure, repository.refreshByNameOrId("bulbasaur"))
        assertEquals(failure, repository.refreshDetail(1))

        repository.failWith = null
        assertEquals(DataResult.Success(emptyList<PokemonSummary>()), repository.observeList(10).first())
        assertEquals(DataResult.Failure(DataError.NotFound), repository.observeByNameOrId("1").first())
    }

    @Test
    fun `observeByNameOrId finds by number or name ignoring case and spaces`() = runTest {
        val repository = FakePokemonRepository()
        repository.seedSummary(PokemonFixtures.mrMime)
        repository.seedList(listOf(a, b), offset = 0, totalCount = 2)
        assertEquals(b, repository.observeByNameOrId(" 002 ").first().value())
        assertEquals(b, repository.observeByNameOrId("  IvySaur ").first().value())
        assertEquals(PokemonFixtures.mrMime, repository.observeByNameOrId("mr-mime").first().value())
        assertEquals(DataResult.Failure(DataError.NotFound), repository.observeByNameOrId("   ").first())
    }

    @Test
    fun `observeSummaries keeps the requested order and skips missing ids`() = runTest {
        val repository = FakePokemonRepository()
        repository.seedList(listOf(a, b, c), offset = 0, totalCount = 3)
        assertEquals(listOf(c, a, b), repository.observeSummaries(listOf(3, 1, 2)).first().value())
        assertEquals(listOf(c, a), repository.observeSummaries(listOf(3, 404, 1)).first().value())
    }

    @Test
    fun `a stored detail follows a renamed summary`() = runTest {
        val repository = FakePokemonRepository()
        repository.seedDetail(PokemonFixtures.bulbasaurDetail)
        val renamed = a.copy(name = "bulba")
        repository.seedSummary(renamed)
        assertEquals(PokemonFixtures.bulbasaurDetail.copy(summary = renamed), repository.observeDetail(1).first().value())
    }

    @Test
    fun `observers see later writes`() = runTest {
        val repository = fakeWithServer()
        val flow = repository.observeList(10)
        assertEquals(emptyList<PokemonSummary>(), flow.first().value())
        repository.refreshPage(0, 20)
        assertEquals(listOf(a, b, c), flow.first().value())
    }
}

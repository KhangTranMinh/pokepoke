package com.khangtm.pokepoke.data.pokemon.api

import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Rules every [PokemonRepository] must follow. Extend it for the Room implementation and for the fake, so both
 * are verified against the same tests.
 */
abstract class PokemonRepositoryContract {

    abstract fun createRepository(): PokemonRepository

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

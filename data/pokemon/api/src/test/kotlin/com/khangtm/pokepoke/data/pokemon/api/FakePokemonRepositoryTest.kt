package com.khangtm.pokepoke.data.pokemon.api

import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** The fake must follow the same contract as the Room implementation. */
class FakePokemonRepositoryContractTest : PokemonRepositoryContract() {
    override fun createRepository(): PokemonRepository = FakePokemonRepository()
}

class FakePokemonRepositoryTest {

    private val error = DataError.Local.Read(IllegalStateException("boom"))

    @Test
    fun `failWith makes every function fail`() = runTest {
        val repository = FakePokemonRepository()
        repository.failWith = error
        val failure = DataResult.Failure(error)

        assertEquals(failure, repository.observeList(10).first())
        assertEquals(failure, repository.observeTotalCount().first())
        assertEquals(failure, repository.observeDetail(1).first())
        assertEquals(failure, repository.observeByNameOrId("1").first())
        assertEquals(failure, repository.observeSummaries(listOf(1)).first())
        assertEquals(failure, repository.saveList(emptyList(), 0, 0))
        assertEquals(failure, repository.saveSummary(PokemonFixtures.bulbasaur))
        assertEquals(failure, repository.saveDetail(PokemonFixtures.bulbasaurDetail))
    }

    @Test
    fun `a failed write stores nothing and clearing failWith recovers`() = runTest {
        val repository = FakePokemonRepository()
        repository.failWith = error
        repository.saveSummary(PokemonFixtures.bulbasaur)
        repository.failWith = null

        assertEquals(DataResult.Failure(DataError.NotFound), repository.observeByNameOrId("1").first())
    }
}

package com.khangtm.pokepoke.business.domain.favourite

import app.cash.turbine.test
import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeFavouriteRepositoryTest {

    private var now = 10L
    private val repository = FakeFavouriteRepository(clock = { now++ })
    private val error = DataError.Local.Write(IllegalStateException("boom"))

    private suspend fun entries() = (repository.observeFavourites().first() as DataResult.Success).value

    @Test
    fun `empty at the start`() = runTest {
        assertEquals(emptyList<FavouriteEntry>(), entries())
        assertEquals(DataResult.Success(false), repository.observeIsFavourite(1).first())
    }

    @Test
    fun `toggle adds newest first and returns the new state`() = runTest {
        assertEquals(DataResult.Success(true), repository.toggle(1))
        assertEquals(DataResult.Success(true), repository.toggle(2))
        assertEquals(listOf(FavouriteEntry(2, 11), FavouriteEntry(1, 10)), entries())
    }

    @Test
    fun `toggling twice restores the original state`() = runTest {
        repository.toggle(1)
        val before = entries()
        assertEquals(DataResult.Success(true), repository.toggle(2))
        assertEquals(DataResult.Success(false), repository.toggle(2))
        assertEquals(before, entries())
    }

    @Test
    fun `remove and clear`() = runTest {
        repository.toggle(1)
        repository.toggle(2)
        repository.remove(1)
        repository.remove(77)
        assertEquals(listOf(2), entries().map { it.pokemonId })
        repository.clear()
        assertEquals(emptyList<FavouriteEntry>(), entries())
    }

    @Test
    fun `seed keeps the given order`() = runTest {
        val seeded = listOf(FavouriteEntry(3, 30), FavouriteEntry(1, 5))
        repository.seed(seeded)
        assertEquals(seeded, entries())
        assertEquals(DataResult.Success(true), repository.observeIsFavourite(3).first())
    }

    @Test
    fun `observe emits on every change`() = runTest {
        repository.observeIsFavourite(1).test {
            assertEquals(DataResult.Success(false), awaitItem())
            repository.toggle(1)
            assertEquals(DataResult.Success(true), awaitItem())
            repository.toggle(2)
            repository.clear()
            assertEquals(DataResult.Success(false), awaitItem())
        }
    }

    @Test
    fun `failWith makes every function fail and leaves the storage unchanged`() = runTest {
        repository.toggle(1)
        repository.failWith = error
        val failure = DataResult.Failure(error)
        assertEquals(failure, repository.observeFavourites().first())
        assertEquals(failure, repository.observeIsFavourite(1).first())
        assertEquals(failure, repository.toggle(2))
        assertEquals(failure, repository.remove(1))
        assertEquals(failure, repository.clear())
        repository.failWith = null
        assertEquals(listOf(1), entries().map { it.pokemonId })
    }
}

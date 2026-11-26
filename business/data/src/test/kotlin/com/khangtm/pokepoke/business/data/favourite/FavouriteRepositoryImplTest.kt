package com.khangtm.pokepoke.business.data.favourite

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.khangtm.pokepoke.business.data.favourite.db.FavouriteDatabase
import com.khangtm.pokepoke.business.domain.favourite.FavouriteEntry
import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class FavouriteRepositoryImplTest {

    private lateinit var database: FavouriteDatabase
    private lateinit var repository: FavouriteRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            FavouriteDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = FavouriteRepositoryImpl(database.favouriteDao(), UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun <T> DataResult<T>.value(): T = when (this) {
        is DataResult.Success -> value
        is DataResult.Failure -> throw AssertionError("Expected Success but was $error")
    }

    private suspend fun ids(): List<Int> = repository.observeFavourites().first().value().map { it.pokemonId }

    /** `added_at` is the wall clock in milliseconds, so wait a little to get distinct values. */
    private suspend fun add(id: Int) {
        assertEquals(DataResult.Success(true), repository.toggle(id))
        Thread.sleep(3)
    }

    @Test
    fun `nothing stored gives an empty list and not favourite`() = runTest {
        assertEquals(DataResult.Success(emptyList<FavouriteEntry>()), repository.observeFavourites().first())
        assertEquals(DataResult.Success(false), repository.observeIsFavourite(1).first())
    }

    @Test
    fun `favourites come newest first with the time they were added`() = runTest {
        val before = System.currentTimeMillis()
        add(25)
        add(1)
        add(133)
        val entries = repository.observeFavourites().first().value()
        assertEquals(listOf(133, 1, 25), entries.map { it.pokemonId })
        assertTrue(entries.all { it.addedAtEpochMillis >= before })
        assertTrue(entries[0].addedAtEpochMillis > entries[1].addedAtEpochMillis)
        assertTrue(entries[1].addedAtEpochMillis > entries[2].addedAtEpochMillis)
    }

    @Test
    fun `toggle returns the new state`() = runTest {
        assertEquals(DataResult.Success(true), repository.toggle(25))
        assertEquals(DataResult.Success(false), repository.toggle(25))
    }

    @Test
    fun `toggling twice restores the original state`() = runTest {
        add(1)
        add(2)
        val before = ids()
        repository.toggle(3)
        repository.toggle(3)
        assertEquals(before, ids())
        assertEquals(DataResult.Success(false), repository.observeIsFavourite(3).first())
    }

    @Test
    fun `toggling an existing favourite removes it`() = runTest {
        add(1)
        add(2)
        repository.toggle(1)
        assertEquals(listOf(2), ids())
    }

    @Test
    fun `remove deletes one favourite and ignores a missing one`() = runTest {
        add(1)
        add(2)
        assertEquals(DataResult.Success(Unit), repository.remove(1))
        assertEquals(DataResult.Success(Unit), repository.remove(99))
        assertEquals(listOf(2), ids())
    }

    @Test
    fun `clear removes everything`() = runTest {
        add(1)
        add(2)
        assertEquals(DataResult.Success(Unit), repository.clear())
        assertEquals(emptyList<Int>(), ids())
        assertEquals(DataResult.Success(Unit), repository.clear())
    }

    @Test
    fun `observeIsFavourite follows toggle`() = runTest {
        repository.observeIsFavourite(7).test {
            assertEquals(DataResult.Success(false), awaitItem())
            repository.toggle(7)
            assertEquals(DataResult.Success(true), awaitItem())
            repository.remove(7)
            assertEquals(DataResult.Success(false), awaitItem())
        }
    }

    @Test
    fun `observeFavourites follows changes`() = runTest {
        repository.observeFavourites().test {
            assertEquals(emptyList<Int>(), awaitItem().value().map { it.pokemonId })
            repository.toggle(4)
            assertEquals(listOf(4), awaitItem().value().map { it.pokemonId })
            repository.clear()
            assertEquals(emptyList<Int>(), awaitItem().value().map { it.pokemonId })
        }
    }

    @Test
    fun `a closed database reports Local Read from every observe function`() = runTest {
        database.close()
        listOf(
            repository.observeFavourites().first(),
            repository.observeIsFavourite(1).first(),
        ).forEach { result ->
            assertTrue("Expected Local.Read but was $result", result is DataResult.Failure && result.error is DataError.Local.Read)
        }
    }

    @Test
    fun `a closed database reports Local Write from every write function`() = runTest {
        database.close()
        listOf(repository.toggle(1), repository.remove(1), repository.clear()).forEach { result ->
            assertTrue("Expected Local.Write but was $result", result is DataResult.Failure && result.error is DataError.Local.Write)
        }
    }
}

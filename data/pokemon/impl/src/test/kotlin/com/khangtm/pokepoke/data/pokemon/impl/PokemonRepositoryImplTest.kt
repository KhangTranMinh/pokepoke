package com.khangtm.pokepoke.data.pokemon.impl

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import com.khangtm.pokepoke.data.pokemon.api.PokemonFixtures
import com.khangtm.pokepoke.data.pokemon.api.PokemonRepository
import com.khangtm.pokepoke.data.pokemon.api.PokemonRepositoryContract
import com.khangtm.pokepoke.data.pokemon.impl.db.PokemonDao
import com.khangtm.pokepoke.data.pokemon.impl.db.PokemonDatabase
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

@OptIn(ExperimentalCoroutinesApi::class)
private fun createInMemoryDatabase(): PokemonDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), PokemonDatabase::class.java)
        .allowMainThreadQueries()
        .build()

@OptIn(ExperimentalCoroutinesApi::class)
private fun createRepository(dao: PokemonDao): PokemonRepositoryImpl =
    PokemonRepositoryImpl(dao, UnconfinedTestDispatcher())

/** The Room implementation must follow the same contract as the fake. */
@RunWith(RobolectricTestRunner::class)
class PokemonRepositoryImplContractTest : PokemonRepositoryContract() {

    private lateinit var database: PokemonDatabase

    @Before
    fun openDatabase() {
        database = createInMemoryDatabase()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    override fun createRepository(): PokemonRepository = createRepository(database.pokemonDao())
}

/** Every function of a repository whose database is closed must report the error instead of throwing. */
@RunWith(RobolectricTestRunner::class)
class PokemonRepositoryImplErrorTest {

    private lateinit var database: PokemonDatabase
    private lateinit var repository: PokemonRepository

    @Before
    fun openAndCloseDatabase() {
        database = createInMemoryDatabase()
        repository = createRepository(database.pokemonDao())
        database.close()
    }

    private suspend fun assertReadFailure(result: DataResult<*>) {
        assertTrue("Expected Local.Read but was $result", result is DataResult.Failure && result.error is DataError.Local.Read)
    }

    private fun assertWriteFailure(result: DataResult<*>) {
        assertTrue("Expected Local.Write but was $result", result is DataResult.Failure && result.error is DataError.Local.Write)
    }

    @Test
    fun `every observe function reports Local Read when the database fails`() = runTest {
        assertReadFailure(repository.observeList(10).first())
        assertReadFailure(repository.observeTotalCount().first())
        assertReadFailure(repository.observeDetail(1).first())
        assertReadFailure(repository.observeByNameOrId("1").first())
        assertReadFailure(repository.observeByNameOrId("bulbasaur").first())
        assertReadFailure(repository.observeSummaries(listOf(1, 2)).first())
    }

    @Test
    fun `every save function reports Local Write when the database fails`() = runTest {
        assertWriteFailure(repository.saveList(listOf(PokemonFixtures.bulbasaur), 0, 1))
        assertWriteFailure(repository.saveSummary(PokemonFixtures.bulbasaur))
        assertWriteFailure(repository.saveDetail(PokemonFixtures.bulbasaurDetail))
    }
}

@RunWith(RobolectricTestRunner::class)
class PokemonRepositoryImplCorruptionTest {

    private lateinit var database: PokemonDatabase

    @Before
    fun openDatabase() {
        database = createInMemoryDatabase()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `a detail row with broken json reports Local Read`() = runTest {
        val codec = PokemonJsonCodec()
        val good = PokemonFixtures.bulbasaurDetail.toEntity(codec, updatedAt = 0)
        database.pokemonDao().saveDetail(PokemonFixtures.bulbasaur.toEntity(), good.copy(typesJson = "not json"))

        val result = createRepository(database.pokemonDao()).observeDetail(1).first()

        assertTrue(result is DataResult.Failure && result.error is DataError.Local.Read)
    }

    @Test
    fun `detail is saved with the current time`() = runTest {
        val repository = createRepository(database.pokemonDao())
        assertEquals(DataResult.Success(Unit), repository.saveDetail(PokemonFixtures.pikachuDetail))
        val row = database.openHelper.readableDatabase.query("SELECT updated_at FROM pokemon_detail").use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }
        assertTrue(row > 0)
    }
}

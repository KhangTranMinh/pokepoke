package com.khangtm.pokepoke.business.domain.pokemon

import app.cash.turbine.test
import com.khangtm.pokepoke.core.common.AppError
import com.khangtm.pokepoke.core.common.AppResult
import com.khangtm.pokepoke.core.common.DataError
import com.khangtm.pokepoke.core.common.DataResult
import com.khangtm.pokepoke.core.common.DataSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private fun page(from: Int, count: Int): List<PokemonSummary> =
    (from until from + count).map { PokemonFixtures.summary(it + 1, "p${it + 1}") }

class ObservePokemonListUseCaseTest {

    @Test
    fun `emits the stored list as AppResult`() = runTest {
        val repository = FakePokemonRepository()
        repository.seedList(listOf(PokemonFixtures.bulbasaur, PokemonFixtures.ivysaur), 0, 2)
        val useCase = ObservePokemonListUseCaseImpl(repository)
        assertEquals(
            AppResult.Success(listOf(PokemonFixtures.bulbasaur, PokemonFixtures.ivysaur)),
            useCase(10).first(),
        )
    }

    @Test
    fun `a read error becomes an AppError`() = runTest {
        val repository = FakePokemonRepository()
        val cause = IllegalStateException("x")
        repository.failWith = DataError.Local.Read(cause)
        assertEquals(AppResult.Failure(AppError.Unknown(cause)), ObservePokemonListUseCaseImpl(repository)(10).first())
    }

    @Test
    fun `follows later changes`() = runTest {
        val repository = FakePokemonRepository()
        ObservePokemonListUseCaseImpl(repository)(10).test {
            assertEquals(AppResult.Success(emptyList<PokemonSummary>()), awaitItem())
            repository.seedList(listOf(PokemonFixtures.bulbasaur), 0, 1)
            assertEquals(AppResult.Success(listOf(PokemonFixtures.bulbasaur)), awaitItem())
        }
    }
}

class LoadPokemonPageUseCaseTest {

    private val repository = FakePokemonRepository().apply { serverList = page(0, 45) }
    private val useCase = LoadPokemonPageUseCaseImpl(repository)

    @Test
    fun `fetched first page shows 20 items and is not the end`() = runTest {
        assertEquals(AppResult.Success(PageResult(20, isOffline = false, endReached = false)), useCase(0))
        assertEquals(1, repository.refreshCalls)
        assertEquals(20, repository.observeList(100).first().let { (it as DataResult.Success).value.size })
    }

    @Test
    fun `fetched last page reaches the end`() = runTest {
        useCase(0)
        useCase(20)
        assertEquals(AppResult.Success(PageResult(60, isOffline = false, endReached = true)), useCase(40))
    }

    @Test
    fun `fetched page that ends exactly at the total reaches the end`() = runTest {
        repository.serverList = page(0, 40)
        useCase(0)
        assertEquals(AppResult.Success(PageResult(40, isOffline = false, endReached = true)), useCase(20))
    }

    @Test
    fun `offline with an extra cached item shows the page as offline`() = runTest {
        repository.seedList(page(0, 25), 0, 45)
        repository.refreshError = DataError.Network.NoConnection
        assertEquals(AppResult.Success(PageResult(40, isOffline = true, endReached = false)), useCase(20))
    }

    @Test
    fun `timeout is treated as offline`() = runTest {
        repository.seedList(page(0, 5), 0, 45)
        repository.refreshError = DataError.Network.Timeout
        assertEquals(AppResult.Success(PageResult(20, isOffline = true, endReached = false)), useCase(0))
    }

    @Test
    fun `offline reports the end when the cached total is reached`() = runTest {
        repository.seedList(page(0, 45), 0, 45)
        repository.refreshError = DataError.Network.NoConnection
        assertEquals(AppResult.Success(PageResult(60, isOffline = true, endReached = true)), useCase(40))
    }

    @Test
    fun `offline without any new item is NoConnection`() = runTest {
        repository.seedList(page(0, 20), 0, 45)
        repository.refreshError = DataError.Network.NoConnection
        assertEquals(AppResult.Failure(AppError.NoConnection), useCase(20))
    }

    @Test
    fun `offline with an empty cache is NoConnection`() = runTest {
        repository.refreshError = DataError.Network.NoConnection
        assertEquals(AppResult.Failure(AppError.NoConnection), useCase(0))
    }

    @Test
    fun `a server error fails without reading the cache`() = runTest {
        repository.seedList(page(0, 40), 0, 45)
        repository.refreshError = DataError.Network.Http(500)
        assertEquals(AppResult.Failure(AppError.Server(500)), useCase(20))
    }

    @Test
    fun `a failure reading the total is returned`() = runTest {
        val cause = IllegalStateException("x")
        val fake = FakePokemonRepository()
        fake.failWith = DataError.Local.Read(cause)
        // failWith makes refresh fail first (Failed), so the total is not read.
        assertEquals(AppResult.Failure(AppError.Unknown(cause)), LoadPokemonPageUseCaseImpl(fake)(0))
    }

    @Test
    fun `a failure reading the total after a successful fetch is returned`() = runTest {
        val cause = IllegalStateException("x")
        val failing = object : PokemonRepository by repository {
            override fun observeTotalCount() = flowOf(
                DataResult.Failure(DataError.Local.Read(cause)),
            )
        }
        assertEquals(AppResult.Failure(AppError.Unknown(cause)), LoadPokemonPageUseCaseImpl(failing)(0))
    }

    @Test
    fun `a failure reading the cached list when offline is returned`() = runTest {
        val cause = IllegalStateException("x")
        repository.refreshError = DataError.Network.NoConnection
        val failing = object : PokemonRepository by repository {
            override fun observeList(limit: Int) = flowOf(
                DataResult.Failure(DataError.Local.Read(cause)),
            )
        }
        assertEquals(AppResult.Failure(AppError.Unknown(cause)), LoadPokemonPageUseCaseImpl(failing)(0))
    }
}

class SearchPokemonUseCaseTest {

    private val repository = FakePokemonRepository().apply {
        serverList = listOf(PokemonFixtures.bulbasaur, PokemonFixtures.pikachu)
    }
    private val useCase = SearchPokemonUseCaseImpl(repository)

    @Test
    fun `blank query is NotFound without calling the repository`() = runTest {
        assertEquals(AppResult.Success(SearchOutcome.NotFound), useCase("   "))
        assertEquals(0, repository.refreshCalls)
    }

    @Test
    fun `fetched pokemon is Found online and the query is normalized`() = runTest {
        assertEquals(
            AppResult.Success(SearchOutcome.Found(PokemonFixtures.pikachu, isOffline = false)),
            useCase("  PikaChu "),
        )
    }

    @Test
    fun `offline with a cached pokemon is Found offline`() = runTest {
        repository.seedSummary(PokemonFixtures.pikachu)
        repository.refreshError = DataError.Network.NoConnection
        assertEquals(
            AppResult.Success(SearchOutcome.Found(PokemonFixtures.pikachu, isOffline = true)),
            useCase("pikachu"),
        )
    }

    @Test
    fun `offline without the pokemon in cache is NoConnection`() = runTest {
        repository.refreshError = DataError.Network.NoConnection
        assertEquals(AppResult.Failure(AppError.NoConnection), useCase("pikachu"))
    }

    @Test
    fun `offline with a cache read error is that error`() = runTest {
        val cause = IllegalStateException("x")
        repository.refreshError = DataError.Network.Timeout
        val failing = object : PokemonRepository by repository {
            override fun observeByNameOrId(query: String) = flowOf(
                DataResult.Failure(DataError.Local.Read(cause)),
            )
        }
        assertEquals(AppResult.Failure(AppError.Unknown(cause)), SearchPokemonUseCaseImpl(failing)("pikachu"))
    }

    @Test
    fun `404 from the API is a NotFound outcome not an error`() = runTest {
        assertEquals(AppResult.Success(SearchOutcome.NotFound), useCase("missingno"))
    }

    @Test
    fun `other errors are failures`() = runTest {
        repository.refreshError = DataError.Network.Http(503)
        assertEquals(AppResult.Failure(AppError.Server(503)), useCase("pikachu"))
        val cause = IllegalStateException("x")
        repository.refreshError = DataError.Network.Parse(cause)
        assertEquals(AppResult.Failure(AppError.Unknown(cause)), useCase("pikachu"))
    }

    @Test
    fun `fetched but unreadable cache is a failure`() = runTest {
        val cause = IllegalStateException("x")
        val failing = object : PokemonRepository by repository {
            override fun observeByNameOrId(query: String) = flowOf(
                DataResult.Failure(DataError.Local.Read(cause)),
            )
        }
        assertEquals(AppResult.Failure(AppError.Unknown(cause)), SearchPokemonUseCaseImpl(failing)("pikachu"))
    }
}

class ObservePokemonDetailUseCaseTest {

    private val repository = FakePokemonRepository().apply {
        serverDetails = mapOf(1 to PokemonFixtures.bulbasaurDetail)
    }
    private val useCase = ObservePokemonDetailUseCaseImpl(repository)

    @Test
    fun `online fetches then emits the detail as not offline`() = runTest {
        assertEquals(
            AppResult.Success(DataSnapshot(PokemonFixtures.bulbasaurDetail, isOffline = false)),
            useCase(1).first(),
        )
        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun `offline with a cached detail emits it as offline`() = runTest {
        repository.seedDetail(PokemonFixtures.bulbasaurDetail)
        repository.refreshError = DataError.Network.NoConnection
        assertEquals(
            AppResult.Success(DataSnapshot(PokemonFixtures.bulbasaurDetail, isOffline = true)),
            useCase(1).first(),
        )
    }

    @Test
    fun `offline without a cached detail is NoConnection`() = runTest {
        repository.refreshError = DataError.Network.Timeout
        assertEquals(AppResult.Failure(AppError.NoConnection), useCase(1).first())
    }

    @Test
    fun `an unknown id is NotFound`() = runTest {
        assertEquals(AppResult.Failure(AppError.NotFound), useCase(999).first())
    }

    @Test
    fun `a server error is reported without reading the cache`() = runTest {
        repository.seedDetail(PokemonFixtures.bulbasaurDetail)
        repository.refreshError = DataError.Network.Http(500)
        useCase(1).test {
            assertEquals(AppResult.Failure(AppError.Server(500)), awaitItem())
            awaitComplete()
        }
    }
}

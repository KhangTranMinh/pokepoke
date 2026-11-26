package com.khangtm.pokepoke.business.domain.favourite

import app.cash.turbine.test
import com.khangtm.pokepoke.business.domain.pokemon.FakePokemonRepository
import com.khangtm.pokepoke.business.domain.pokemon.PokemonFixtures
import com.khangtm.pokepoke.core.common.AppError
import com.khangtm.pokepoke.core.common.AppResult
import com.khangtm.pokepoke.core.common.DataError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveFavouritePokemonUseCaseTest {

    private val favourites = FakeFavouriteRepository(clock = counter())
    private val pokemon = FakePokemonRepository()
    private val useCase = ObserveFavouritePokemonUseCaseImpl(favourites, pokemon)

    private val bulbasaur = PokemonFixtures.bulbasaur
    private val pikachu = PokemonFixtures.pikachu
    private val eevee = PokemonFixtures.eevee

    private fun counter(): () -> Long {
        var now = 100L
        return { now++ }
    }

    private fun success(vararg items: FavouritePokemon) = AppResult.Success(items.toList())

    @Test
    fun `no favourites gives an empty list`() = runTest {
        assertEquals(success(), useCase().first())
    }

    @Test
    fun `newest first with the addedAt of each entry`() = runTest {
        listOf(bulbasaur, pikachu, eevee).forEach { pokemon.seedSummary(it) }
        favourites.toggle(bulbasaur.id)
        favourites.toggle(eevee.id)
        favourites.toggle(pikachu.id)
        assertEquals(
            success(FavouritePokemon(pikachu, 102), FavouritePokemon(eevee, 101), FavouritePokemon(bulbasaur, 100)),
            useCase().first(),
        )
    }

    @Test
    fun `an id without a summary is skipped`() = runTest {
        pokemon.seedSummary(bulbasaur)
        favourites.seed(listOf(FavouriteEntry(999, 5), FavouriteEntry(bulbasaur.id, 4)))
        assertEquals(success(FavouritePokemon(bulbasaur, 4)), useCase().first())
    }

    @Test
    fun `updates when a favourite is added or removed`() = runTest {
        pokemon.seedSummary(bulbasaur)
        pokemon.seedSummary(pikachu)
        useCase().test {
            assertEquals(success(), awaitItem())
            favourites.toggle(bulbasaur.id)
            assertEquals(success(FavouritePokemon(bulbasaur, 100)), awaitItem())
            favourites.toggle(pikachu.id)
            assertEquals(success(FavouritePokemon(pikachu, 101), FavouritePokemon(bulbasaur, 100)), awaitItem())
            favourites.remove(bulbasaur.id)
            assertEquals(success(FavouritePokemon(pikachu, 101)), awaitItem())
        }
    }

    @Test
    fun `updates when a summary appears later`() = runTest {
        favourites.toggle(bulbasaur.id)
        useCase().test {
            assertEquals(success(), awaitItem())
            pokemon.seedSummary(bulbasaur)
            assertEquals(success(FavouritePokemon(bulbasaur, 100)), awaitItem())
        }
    }

    @Test
    fun `a failure of the favourite repository is a failure`() = runTest {
        val cause = IllegalStateException("fav")
        favourites.failWith = DataError.Local.Read(cause)
        assertEquals(AppResult.Failure(AppError.Unknown(cause)), useCase().first())
    }

    @Test
    fun `a failure of the pokemon repository is a failure`() = runTest {
        favourites.toggle(bulbasaur.id)
        pokemon.failWith = DataError.NotFound
        assertEquals(AppResult.Failure(AppError.NotFound), useCase().first())
    }

    @Test
    fun `recovers after the failure is gone`() = runTest {
        pokemon.seedSummary(bulbasaur)
        favourites.toggle(bulbasaur.id)
        useCase().test {
            assertEquals(success(FavouritePokemon(bulbasaur, 100)), awaitItem())
            pokemon.failWith = DataError.Local.Read(IllegalStateException("x"))
            assertEquals(true, awaitItem() is AppResult.Failure)
            pokemon.failWith = null
            assertEquals(success(FavouritePokemon(bulbasaur, 100)), awaitItem())
        }
    }
}

class ObserveIsFavouriteUseCaseTest {

    private val repository = FakeFavouriteRepository()
    private val useCase = ObserveIsFavouriteUseCaseImpl(repository)

    @Test
    fun `follows toggle`() = runTest {
        useCase(1).test {
            assertEquals(AppResult.Success(false), awaitItem())
            repository.toggle(1)
            assertEquals(AppResult.Success(true), awaitItem())
        }
    }

    @Test
    fun `a data error becomes an AppError`() = runTest {
        val cause = IllegalStateException("x")
        repository.failWith = DataError.Local.Read(cause)
        assertEquals(AppResult.Failure(AppError.Unknown(cause)), useCase(1).first())
    }
}

class ToggleFavouriteUseCaseTest {

    private val repository = FakeFavouriteRepository()
    private val useCase = ToggleFavouriteUseCaseImpl(repository)

    @Test
    fun `returns the state after the toggle`() = runTest {
        assertEquals(AppResult.Success(true), useCase(1))
        assertEquals(AppResult.Success(false), useCase(1))
    }

    @Test
    fun `a write error becomes an AppError`() = runTest {
        val cause = IllegalStateException("x")
        repository.failWith = DataError.Local.Write(cause)
        assertEquals(AppResult.Failure(AppError.Unknown(cause)), useCase(1))
    }
}

class RemoveFavouriteUseCaseTest {

    private val repository = FakeFavouriteRepository()
    private val useCase = RemoveFavouriteUseCaseImpl(repository)

    @Test
    fun `removes the favourite`() = runTest {
        repository.toggle(1)
        repository.toggle(2)
        assertEquals(AppResult.Success(Unit), useCase(1))
        assertEquals(listOf(2), (repository.observeFavourites().first() as com.khangtm.pokepoke.core.common.DataResult.Success).value.map { it.pokemonId })
    }

    @Test
    fun `a write error becomes an AppError`() = runTest {
        repository.failWith = DataError.Local.Write(IllegalStateException("x"))
        assertEquals(true, useCase(1) is AppResult.Failure)
    }
}

class ClearFavouritesUseCaseTest {

    private val repository = FakeFavouriteRepository()
    private val useCase = ClearFavouritesUseCaseImpl(repository)

    @Test
    fun `clears all favourites`() = runTest {
        repository.toggle(1)
        assertEquals(AppResult.Success(Unit), useCase())
        assertEquals(AppResult.Success(false), ObserveIsFavouriteUseCaseImpl(repository)(1).first())
    }

    @Test
    fun `a write error becomes an AppError`() = runTest {
        val cause = IllegalStateException("x")
        repository.failWith = DataError.Local.Write(cause)
        assertEquals(AppResult.Failure(AppError.Unknown(cause)), useCase())
    }
}

package com.khangtm.pokepoke.business.domain.favourite

import app.cash.turbine.test
import com.khangtm.pokepoke.business.domain.pokemon.PokemonFixtures
import com.khangtm.pokepoke.core.common.AppError
import com.khangtm.pokepoke.core.common.AppResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeFavouritesTest {

    private val fake = FakeFavourites()

    @Test
    fun `toggle adds to the front and returns the new state`() = runTest {
        assertEquals(AppResult.Success(true), fake.toggle(1))
        assertEquals(AppResult.Success(true), fake.toggle(2))
        assertEquals(listOf(2, 1), fake.ids.value)
        assertEquals(AppResult.Success(false), fake.toggle(2))
        assertEquals(listOf(1), fake.ids.value)
    }

    @Test
    fun `remove and clear change the shared ids`() = runTest {
        fake.ids.value = listOf(3, 2, 1)
        assertEquals(AppResult.Success(Unit), fake.remove(2))
        assertEquals(listOf(3, 1), fake.ids.value)
        assertEquals(AppResult.Success(Unit), fake.clear())
        assertEquals(emptyList<Int>(), fake.ids.value)
    }

    @Test
    fun `observeIsFavourite follows the ids`() = runTest {
        fake.observeIsFavourite(1).test {
            assertEquals(AppResult.Success(false), awaitItem())
            fake.toggle(1)
            assertEquals(AppResult.Success(true), awaitItem())
        }
    }

    @Test
    fun `observeFavouritePokemon keeps the order and skips ids without a summary`() = runTest {
        fake.summaries[1] = PokemonFixtures.bulbasaur
        fake.summaries[25] = PokemonFixtures.pikachu
        fake.ids.value = listOf(25, 999, 1)
        val result = fake.observeFavouritePokemon().first() as AppResult.Success
        assertEquals(listOf(PokemonFixtures.pikachu, PokemonFixtures.bulbasaur), result.value.map { it.pokemon })
        assertEquals(true, result.value[0].addedAtEpochMillis > result.value[1].addedAtEpochMillis)
    }

    @Test
    fun `observeFavouritePokemon updates when the ids change`() = runTest {
        fake.summaries[1] = PokemonFixtures.bulbasaur
        fake.observeFavouritePokemon().test {
            assertEquals(emptyList<FavouritePokemon>(), (awaitItem() as AppResult.Success).value)
            fake.toggle(1)
            assertEquals(listOf(PokemonFixtures.bulbasaur), (awaitItem() as AppResult.Success).value.map { it.pokemon })
        }
    }

    @Test
    fun `failWith makes every use case fail and leaves the ids unchanged`() = runTest {
        fake.ids.value = listOf(1)
        fake.failWith = AppError.NotFound
        val failure = AppResult.Failure(AppError.NotFound)
        assertEquals(failure, fake.observeFavouritePokemon().first())
        assertEquals(failure, fake.observeIsFavourite(1).first())
        assertEquals(failure, fake.toggle(2))
        assertEquals(failure, fake.remove(1))
        assertEquals(failure, fake.clear())
        assertEquals(listOf(1), fake.ids.value)
    }
}

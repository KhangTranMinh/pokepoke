package com.khangtm.pokepoke.data.pokemon.api

import org.junit.Assert.assertEquals
import org.junit.Test

class PokemonFormatTest {

    @Test
    fun `displayId pads to four digits`() {
        assertEquals("#0001", PokemonFormat.displayId(1))
        assertEquals("#0025", PokemonFormat.displayId(25))
    }

    @Test
    fun `displayId keeps ids longer than four digits`() {
        assertEquals("#1025", PokemonFormat.displayId(1025))
        assertEquals("#10001", PokemonFormat.displayId(10001))
    }

    @Test
    fun `displayName capitalizes every hyphen separated word`() {
        assertEquals("Pikachu", PokemonFormat.displayName("pikachu"))
        assertEquals("Mr Mime", PokemonFormat.displayName("mr-mime"))
        assertEquals("Ho Oh Special", PokemonFormat.displayName("ho-oh-special"))
    }

    @Test
    fun `displayName of an empty name is empty`() {
        assertEquals("", PokemonFormat.displayName(""))
    }
}

package com.khangtm.pokepoke.business.data.pokemon

import com.khangtm.pokepoke.business.domain.pokemon.PokemonFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PokemonImageUrlsTest {

    @Test
    fun `domain test fixtures use the same image url convention`() {
        listOf(1, 25, 10001).forEach {
            assertEquals(PokemonImageUrls.artworkUrl(it), PokemonFixtures.summary(it, "x").imageUrl)
        }
    }

    @Test
    fun `artworkUrl appends the id and png to the base url`() {
        assertEquals(
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/25.png",
            PokemonImageUrls.artworkUrl(25),
        )
        assertEquals(PokemonImageUrls.ARTWORK_BASE_URL + "10001.png", PokemonImageUrls.artworkUrl(10001))
    }

    @Test
    fun `id is parsed from a pokemon url with a trailing slash`() {
        assertEquals(25, PokemonImageUrls.idFromResourceUrl("https://pokeapi.co/api/v2/pokemon/25/"))
    }

    @Test
    fun `id is parsed from a pokemon url without a trailing slash`() {
        assertEquals(25, PokemonImageUrls.idFromResourceUrl("https://pokeapi.co/api/v2/pokemon/25"))
    }

    @Test
    fun `id is parsed from species and evolution chain urls`() {
        assertEquals(25, PokemonImageUrls.idFromResourceUrl("https://pokeapi.co/api/v2/pokemon-species/25/"))
        assertEquals(10, PokemonImageUrls.idFromResourceUrl("https://pokeapi.co/api/v2/evolution-chain/10/"))
        assertEquals(10001, PokemonImageUrls.idFromResourceUrl("https://pokeapi.co/api/v2/pokemon/10001/"))
    }

    @Test
    fun `id is parsed when several trailing slashes are present`() {
        assertEquals(7, PokemonImageUrls.idFromResourceUrl("https://pokeapi.co/api/v2/pokemon/7//"))
    }

    @Test
    fun `malformed urls give null`() {
        assertNull(PokemonImageUrls.idFromResourceUrl(""))
        assertNull(PokemonImageUrls.idFromResourceUrl("/"))
        assertNull(PokemonImageUrls.idFromResourceUrl("25"))
        assertNull(PokemonImageUrls.idFromResourceUrl("https://pokeapi.co/api/v2/pokemon/"))
        assertNull(PokemonImageUrls.idFromResourceUrl("https://pokeapi.co/api/v2/pokemon/pikachu/"))
        assertNull(PokemonImageUrls.idFromResourceUrl("https://pokeapi.co/api/v2/pokemon/25a/"))
        assertNull(PokemonImageUrls.idFromResourceUrl("https://pokeapi.co/api/v2/pokemon/-5/"))
        assertNull(PokemonImageUrls.idFromResourceUrl("https://pokeapi.co/api/v2/pokemon/0/"))
        assertNull(PokemonImageUrls.idFromResourceUrl("https://pokeapi.co/api/v2/pokemon/99999999999/"))
    }
}

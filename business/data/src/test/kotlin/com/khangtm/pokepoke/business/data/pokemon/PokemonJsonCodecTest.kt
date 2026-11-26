package com.khangtm.pokepoke.business.data.pokemon

import com.khangtm.pokepoke.business.domain.pokemon.EvolutionTrigger
import com.khangtm.pokepoke.business.domain.pokemon.PokemonFixtures
import com.khangtm.pokepoke.business.domain.pokemon.PokemonType
import com.khangtm.pokepoke.business.domain.pokemon.StatType
import org.junit.Assert.assertEquals
import org.junit.Test

class PokemonJsonCodecTest {

    private val codec = PokemonJsonCodec()

    @Test
    fun `collections round trip`() {
        val detail = PokemonFixtures.bulbasaurDetail
        assertEquals(detail.types, codec.decodeTypes(codec.encodeTypes(detail.types)))
        assertEquals(detail.abilities, codec.decodeAbilities(codec.encodeAbilities(detail.abilities)))
        assertEquals(detail.stats, codec.decodeStats(codec.encodeStats(detail.stats)))
    }

    @Test
    fun `evolution tree round trips for every trigger`() {
        listOf(PokemonFixtures.bulbasaurChain, PokemonFixtures.pikachuChain, PokemonFixtures.eeveeChain).forEach {
            assertEquals(it, codec.decodeEvolution(codec.encodeEvolution(it)))
        }
        assertEquals(
            setOf(EvolutionTrigger.LEVEL_UP, EvolutionTrigger.USE_ITEM, EvolutionTrigger.OTHER),
            generateSequence(listOf(PokemonFixtures.pikachuChain, PokemonFixtures.bulbasaurChain)) { stages ->
                stages.flatMap { it.evolvesTo }.takeIf { it.isNotEmpty() }
            }.flatten().mapNotNull { it.condition?.trigger }.toSet(),
        )
    }

    @Test
    fun `unknown keys decode to UNKNOWN or OTHER`() {
        assertEquals(listOf(PokemonType.UNKNOWN), codec.decodeTypes("[\"shadow\"]"))
        assertEquals(StatType.UNKNOWN, codec.decodeStats("[{\"type\":\"accuracy\",\"baseValue\":1}]").single().type)
        val stage = codec.decodeEvolution(
            "{\"pokemon\":{\"id\":1,\"name\":\"a\",\"imageUrl\":\"u\"}," +
                "\"condition\":{\"trigger\":\"SPIN\",\"minLevel\":null,\"item\":null},\"evolvesTo\":[],\"extra\":1}",
        )
        assertEquals(EvolutionTrigger.OTHER, stage.condition?.trigger)
    }
}

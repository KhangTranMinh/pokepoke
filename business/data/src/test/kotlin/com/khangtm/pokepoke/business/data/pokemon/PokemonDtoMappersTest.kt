package com.khangtm.pokepoke.business.data.pokemon

import com.khangtm.pokepoke.business.domain.pokemon.EvolutionCondition
import com.khangtm.pokepoke.business.domain.pokemon.EvolutionStage
import com.khangtm.pokepoke.business.domain.pokemon.EvolutionTrigger
import com.khangtm.pokepoke.business.domain.pokemon.PokemonAbility
import com.khangtm.pokepoke.business.domain.pokemon.PokemonStat
import com.khangtm.pokepoke.business.domain.pokemon.PokemonSummary
import com.khangtm.pokepoke.business.domain.pokemon.PokemonType
import com.khangtm.pokepoke.business.domain.pokemon.StatType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PokemonDtoMappersTest {

    private fun summary(id: Int, name: String) = PokemonSummary(id, name, PokemonImageUrls.artworkUrl(id))

    @Test
    fun `list item maps to a summary with the artwork url`() {
        val items = PokemonJsonSamples.list("pokemon_list.json").results.mapNotNull { it.toSummary() }
        assertEquals(listOf(summary(1, "bulbasaur"), summary(2, "ivysaur"), summary(3, "venusaur")), items)
    }

    @Test
    fun `list item with an unparseable id is skipped`() {
        val items = PokemonJsonSamples.list("pokemon_list_unparseable.json").results.mapNotNull { it.toSummary() }
        assertEquals(listOf(summary(1, "bulbasaur"), summary(3, "venusaur")), items)
    }

    @Test
    fun `basic dto maps to a summary`() {
        assertEquals(summary(25, "pikachu"), PokemonBasicDto(25, "pikachu").toSummary())
    }

    @Test
    fun `a basic dto can be read from a full pokemon response`() {
        val basic = Json { ignoreUnknownKeys = true }
            .decodeFromString<PokemonBasicDto>(PokemonJsonSamples.text("pokemon_25.json"))
        assertEquals(summary(25, "pikachu"), basic.toSummary())
    }

    @Test
    fun `single stage chain maps to a root without children`() {
        val detail = PokemonJsonSamples.pokemon("pokemon_no_sprites_other.json")
            .toDetail(PokemonJsonSamples.chain("evolution_chain_single.json"))
        assertEquals(EvolutionStage(summary(132, "ditto"), null, emptyList()), detail.evolutionChain)
    }

    @Test
    fun `linear chain keeps conditions on every stage but the root`() {
        val detail = PokemonJsonSamples.pokemon("pokemon_1.json").toDetail(PokemonJsonSamples.chain("evolution_chain_1.json"))
        val expected = EvolutionStage(
            summary(1, "bulbasaur"), null,
            listOf(
                EvolutionStage(
                    summary(2, "ivysaur"), EvolutionCondition(EvolutionTrigger.LEVEL_UP, 16, null),
                    listOf(EvolutionStage(summary(3, "venusaur"), EvolutionCondition(EvolutionTrigger.LEVEL_UP, 32, null), emptyList())),
                ),
            ),
        )
        assertEquals(expected, detail.evolutionChain)
    }

    @Test
    fun `eevee chain has eight branches with their triggers and items`() {
        val detail = PokemonJsonSamples.pokemon("pokemon_no_sprites_other.json").toDetail(PokemonJsonSamples.chain("evolution_chain_67.json"))
        val root = detail.evolutionChain
        assertEquals(summary(133, "eevee"), root.pokemon)
        assertNull(root.condition)
        assertEquals(
            listOf("vaporeon", "jolteon", "flareon", "espeon", "umbreon", "leafeon", "glaceon", "sylveon"),
            root.evolvesTo.map { it.pokemon.name },
        )
        assertEquals(EvolutionCondition(EvolutionTrigger.USE_ITEM, null, "water-stone"), root.evolvesTo[0].condition)
        assertEquals(EvolutionCondition(EvolutionTrigger.LEVEL_UP, null, null), root.evolvesTo[3].condition)
        assertEquals(summary(700, "sylveon"), root.evolvesTo[7].pokemon)
    }

    @Test
    fun `pichu pikachu raichu maps use item and a level up without level`() {
        val detail = PokemonJsonSamples.pokemon("pokemon_25.json").toDetail(PokemonJsonSamples.chain("evolution_chain_10.json"))
        val pikachu = detail.evolutionChain.evolvesTo.single()
        assertEquals(EvolutionCondition(EvolutionTrigger.LEVEL_UP, null, null), pikachu.condition)
        assertEquals(EvolutionCondition(EvolutionTrigger.USE_ITEM, null, "thunder-stone"), pikachu.evolvesTo.single().condition)
    }

    @Test
    fun `a child stage whose id cannot be parsed is skipped`() {
        val detail = PokemonJsonSamples.pokemon("pokemon_1.json").toDetail(PokemonJsonSamples.chain("evolution_chain_bad_child.json"))
        assertEquals(listOf("ivysaur"), detail.evolutionChain.evolvesTo.map { it.pokemon.name })
        assertEquals(EvolutionTrigger.TRADE, detail.evolutionChain.evolvesTo.single().condition?.trigger)
    }

    @Test
    fun `a root stage whose id cannot be parsed is an error`() {
        assertThrows(IllegalArgumentException::class.java) {
            PokemonJsonSamples.pokemon("pokemon_1.json").toDetail(PokemonJsonSamples.chain("evolution_chain_bad_root.json"))
        }
    }

    @Test
    fun `detail maps fields and orders types and abilities by slot`() {
        val detail = PokemonJsonSamples.pokemon("pokemon_1.json").toDetail(PokemonJsonSamples.chain("evolution_chain_1.json"))
        assertEquals(summary(1, "bulbasaur"), detail.summary)
        assertEquals(7, detail.heightDecimeters)
        assertEquals(69, detail.weightHectograms)
        assertEquals(64, detail.baseExperience)
        assertEquals(listOf(PokemonType.GRASS, PokemonType.POISON), detail.types)
        assertEquals(listOf(PokemonAbility("overgrow", false), PokemonAbility("chlorophyll", true)), detail.abilities)
        assertEquals(PokemonStat(StatType.HP, 45), detail.stats.first())
        assertEquals(PokemonStat(StatType.SPEED, 45), detail.stats.last())
        assertEquals(6, detail.stats.size)
    }

    @Test
    fun `missing artwork falls back to the conventional url`() {
        val chain = PokemonJsonSamples.chain("evolution_chain_single.json")
        val nullArtwork = PokemonJsonSamples.pokemon("pokemon_missing_artwork.json").toDetail(chain)
        val noOther = PokemonJsonSamples.pokemon("pokemon_no_sprites_other.json").toDetail(chain)
        assertEquals(PokemonImageUrls.artworkUrl(10001), nullArtwork.summary.imageUrl)
        assertEquals(PokemonImageUrls.artworkUrl(133), noOther.summary.imageUrl)
        assertNull(noOther.baseExperience)
    }

    @Test
    fun `artwork from the response is used when present`() {
        val detail = PokemonJsonSamples.pokemon("pokemon_25.json").toDetail(PokemonJsonSamples.chain("evolution_chain_10.json"))
        assertEquals(PokemonImageUrls.artworkUrl(25), detail.summary.imageUrl)
    }

    @Test
    fun `species and chain ids are parsed from urls`() {
        assertEquals(25, PokemonJsonSamples.pokemon("pokemon_25.json").speciesId())
        assertEquals(10, PokemonJsonSamples.species("species_25.json").evolutionChainId())
        assertEquals(67, PokemonJsonSamples.species("species_133.json").evolutionChainId())
    }

    @Test
    fun `an unparseable species url is an error`() {
        assertThrows(IllegalArgumentException::class.java) { PokemonJsonSamples.pokemon("pokemon_bad_species_url.json").speciesId() }
    }
}

package com.khangtm.pokepoke.data.pokemon.api

import org.junit.Assert.assertEquals
import org.junit.Test

class PokemonEnumsTest {

    @Test
    fun `every pokemon type is found by its own key`() {
        PokemonType.entries.forEach { assertEquals(it, PokemonType.fromKey(it.key)) }
    }

    @Test
    fun `an unknown pokemon type key gives UNKNOWN`() {
        assertEquals(PokemonType.UNKNOWN, PokemonType.fromKey("shadow"))
        assertEquals(PokemonType.UNKNOWN, PokemonType.fromKey(""))
        assertEquals(PokemonType.UNKNOWN, PokemonType.fromKey("FIRE"))
    }

    @Test
    fun `every stat type is found by its own key`() {
        StatType.entries.forEach { assertEquals(it, StatType.fromKey(it.key)) }
        assertEquals(StatType.SPECIAL_ATTACK, StatType.fromKey("special-attack"))
    }

    @Test
    fun `an unknown stat key gives UNKNOWN`() {
        assertEquals(StatType.UNKNOWN, StatType.fromKey("accuracy"))
    }

    @Test
    fun `evolution trigger keys map to their trigger and the rest to OTHER`() {
        assertEquals(EvolutionTrigger.LEVEL_UP, EvolutionTrigger.fromKey("level-up"))
        assertEquals(EvolutionTrigger.USE_ITEM, EvolutionTrigger.fromKey("use-item"))
        assertEquals(EvolutionTrigger.TRADE, EvolutionTrigger.fromKey("trade"))
        assertEquals(EvolutionTrigger.OTHER, EvolutionTrigger.fromKey("shed"))
        assertEquals(EvolutionTrigger.OTHER, EvolutionTrigger.fromKey(""))
    }
}

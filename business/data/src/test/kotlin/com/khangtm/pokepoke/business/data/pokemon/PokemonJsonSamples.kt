package com.khangtm.pokepoke.business.data.pokemon

import kotlinx.serialization.json.Json

/** JSON samples shaped like the real PokeAPI responses (trimmed), in `src/test/resources`. */
internal object PokemonJsonSamples {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

    fun text(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource(name)) { "Missing test resource $name" }.readText()

    fun pokemon(name: String): PokemonDto = json.decodeFromString(text(name))

    fun species(name: String): SpeciesDto = json.decodeFromString(text(name))

    fun chain(name: String): EvolutionChainDto = json.decodeFromString(text(name))

    fun list(name: String): PokemonListDto = json.decodeFromString(text(name))
}

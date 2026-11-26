package com.khangtm.pokepoke.data.pokemon.api

/** Display format of data (not UI text). */
object PokemonFormat {
    /** #0025, #1025, #10001. */
    fun displayId(id: Int): String = "#" + id.toString().padStart(4, '0')

    /** "mr-mime" → "Mr Mime", "pikachu" → "Pikachu". */
    fun displayName(apiName: String): String =
        apiName.split('-').joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
}

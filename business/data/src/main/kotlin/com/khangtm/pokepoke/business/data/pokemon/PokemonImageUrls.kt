package com.khangtm.pokepoke.business.data.pokemon

/** The only place that knows the image URL convention of PokeAPI. */
internal object PokemonImageUrls {
    const val ARTWORK_BASE_URL =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"

    fun artworkUrl(id: Int): String = "$ARTWORK_BASE_URL$id.png"

    /**
     * ".../pokemon/25/", ".../pokemon-species/25/", ".../evolution-chain/10/" (trailing slash optional) → id.
     * Returns null when the last path segment is not a positive integer or there is no path.
     */
    fun idFromResourceUrl(url: String): Int? {
        val trimmed = url.trimEnd('/')
        if (!trimmed.contains('/')) return null
        val last = trimmed.substringAfterLast('/')
        if (last.isEmpty() || !last.all { it in '0'..'9' }) return null
        return last.toIntOrNull()?.takeIf { it > 0 }
    }
}

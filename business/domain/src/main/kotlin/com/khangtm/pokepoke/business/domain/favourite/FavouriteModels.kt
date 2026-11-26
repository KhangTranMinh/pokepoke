package com.khangtm.pokepoke.business.domain.favourite

import com.khangtm.pokepoke.business.domain.pokemon.PokemonSummary

data class FavouriteEntry(val pokemonId: Int, val addedAtEpochMillis: Long)

data class FavouritePokemon(val pokemon: PokemonSummary, val addedAtEpochMillis: Long)

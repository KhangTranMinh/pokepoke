package com.khangtm.pokepoke.business.data.favourite

import com.khangtm.pokepoke.business.data.favourite.db.FavouriteEntity
import com.khangtm.pokepoke.business.domain.favourite.FavouriteEntry

internal fun FavouriteEntity.toModel(): FavouriteEntry = FavouriteEntry(pokemonId, addedAt)

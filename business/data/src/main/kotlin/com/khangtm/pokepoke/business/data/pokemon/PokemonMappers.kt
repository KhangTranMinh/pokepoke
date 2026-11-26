package com.khangtm.pokepoke.business.data.pokemon

import com.khangtm.pokepoke.business.domain.pokemon.PokemonDetail
import com.khangtm.pokepoke.business.domain.pokemon.PokemonSummary
import com.khangtm.pokepoke.business.data.pokemon.db.PokemonDetailEntity
import com.khangtm.pokepoke.business.data.pokemon.db.PokemonDetailRow
import com.khangtm.pokepoke.business.data.pokemon.db.PokemonEntity

internal fun PokemonEntity.toModel(): PokemonSummary = PokemonSummary(id = id, name = name, imageUrl = imageUrl)

internal fun PokemonSummary.toEntity(listPosition: Int? = null): PokemonEntity =
    PokemonEntity(id = id, name = name, imageUrl = imageUrl, listPosition = listPosition)

internal fun PokemonDetailRow.toModel(codec: PokemonJsonCodec): PokemonDetail = PokemonDetail(
    summary = PokemonSummary(id = id, name = name, imageUrl = imageUrl),
    heightDecimeters = height,
    weightHectograms = weight,
    baseExperience = baseExperience,
    types = codec.decodeTypes(typesJson),
    abilities = codec.decodeAbilities(abilitiesJson),
    stats = codec.decodeStats(statsJson),
    evolutionChain = codec.decodeEvolution(evolutionJson),
)

internal fun PokemonDetail.toEntity(codec: PokemonJsonCodec, updatedAt: Long): PokemonDetailEntity =
    PokemonDetailEntity(
        pokemonId = summary.id,
        height = heightDecimeters,
        weight = weightHectograms,
        baseExperience = baseExperience,
        typesJson = codec.encodeTypes(types),
        abilitiesJson = codec.encodeAbilities(abilities),
        statsJson = codec.encodeStats(stats),
        evolutionJson = codec.encodeEvolution(evolutionChain),
        updatedAt = updatedAt,
    )

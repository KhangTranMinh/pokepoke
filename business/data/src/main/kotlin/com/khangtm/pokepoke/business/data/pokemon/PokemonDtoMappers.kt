package com.khangtm.pokepoke.business.data.pokemon

import com.khangtm.pokepoke.business.domain.pokemon.EvolutionCondition
import com.khangtm.pokepoke.business.domain.pokemon.EvolutionStage
import com.khangtm.pokepoke.business.domain.pokemon.EvolutionTrigger
import com.khangtm.pokepoke.business.domain.pokemon.PokemonAbility
import com.khangtm.pokepoke.business.domain.pokemon.PokemonDetail
import com.khangtm.pokepoke.business.domain.pokemon.PokemonStat
import com.khangtm.pokepoke.business.domain.pokemon.PokemonSummary
import com.khangtm.pokepoke.business.domain.pokemon.PokemonType
import com.khangtm.pokepoke.business.domain.pokemon.StatType

/** Null when the id cannot be read from the resource url: such an item is skipped. */
internal fun NamedResourceDto.toSummary(): PokemonSummary? =
    PokemonImageUrls.idFromResourceUrl(url)?.let { id ->
        PokemonSummary(id = id, name = name, imageUrl = PokemonImageUrls.artworkUrl(id))
    }

internal fun PokemonBasicDto.toSummary(): PokemonSummary =
    PokemonSummary(id = id, name = name, imageUrl = PokemonImageUrls.artworkUrl(id))

/** Id of the species of this Pokémon, parsed from its resource url. Throws [IllegalArgumentException] when invalid. */
internal fun PokemonDto.speciesId(): Int = resourceId(species.url)

internal fun SpeciesDto.evolutionChainId(): Int = resourceId(evolutionChain.url)

private fun resourceId(url: String): Int =
    requireNotNull(PokemonImageUrls.idFromResourceUrl(url)) { "Cannot read an id from '$url'" }

/** Throws [IllegalArgumentException] when a required id cannot be parsed (reported as `Network.Parse`). */
internal fun PokemonDto.toDetail(chain: EvolutionChainDto): PokemonDetail = PokemonDetail(
    summary = PokemonSummary(
        id = id,
        name = name,
        imageUrl = sprites.other?.officialArtwork?.frontDefault ?: PokemonImageUrls.artworkUrl(id),
    ),
    heightDecimeters = height,
    weightHectograms = weight,
    baseExperience = baseExperience,
    types = types.sortedBy { it.slot }.map { PokemonType.fromKey(it.type.name) },
    abilities = abilities.sortedBy { it.slot }.map { PokemonAbility(it.ability.name, it.isHidden) },
    stats = stats.map { PokemonStat(StatType.fromKey(it.stat.name), it.baseStat) },
    evolutionChain = chain.chain.toStage(isRoot = true),
)

private fun ChainLinkDto.toStage(isRoot: Boolean): EvolutionStage {
    val speciesId = resourceId(species.url)
    return EvolutionStage(
        pokemon = PokemonSummary(speciesId, species.name, PokemonImageUrls.artworkUrl(speciesId)),
        condition = if (isRoot) null else evolutionDetails.firstOrNull()?.toCondition(),
        // A child whose id cannot be read is skipped.
        evolvesTo = evolvesTo.filter { PokemonImageUrls.idFromResourceUrl(it.species.url) != null }
            .map { it.toStage(isRoot = false) },
    )
}

private fun EvolutionDetailDto.toCondition(): EvolutionCondition = EvolutionCondition(
    trigger = EvolutionTrigger.fromKey(trigger.name),
    minLevel = minLevel,
    item = item?.name,
)

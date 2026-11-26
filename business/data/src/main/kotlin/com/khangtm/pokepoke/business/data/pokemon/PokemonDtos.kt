package com.khangtm.pokepoke.business.data.pokemon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PokemonDto(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    @SerialName("base_experience") val baseExperience: Int? = null,
    val types: List<TypeSlotDto>,
    val abilities: List<AbilitySlotDto>,
    val stats: List<StatDto>,
    val sprites: SpritesDto,
    val species: NamedResourceDto,
)

@Serializable
internal data class PokemonListDto(val count: Int, val results: List<NamedResourceDto>)

@Serializable
internal data class NamedResourceDto(val name: String, val url: String)

@Serializable
internal data class PokemonBasicDto(val id: Int, val name: String)

@Serializable
internal data class TypeSlotDto(val slot: Int, val type: NamedResourceDto)

@Serializable
internal data class AbilitySlotDto(
    val slot: Int,
    @SerialName("is_hidden") val isHidden: Boolean,
    val ability: NamedResourceDto,
)

@Serializable
internal data class StatDto(@SerialName("base_stat") val baseStat: Int, val stat: NamedResourceDto)

@Serializable
internal data class SpritesDto(val other: OtherSpritesDto? = null)

@Serializable
internal data class OtherSpritesDto(@SerialName("official-artwork") val officialArtwork: ArtworkDto? = null)

@Serializable
internal data class ArtworkDto(@SerialName("front_default") val frontDefault: String? = null)

@Serializable
internal data class SpeciesDto(@SerialName("evolution_chain") val evolutionChain: UrlDto)

@Serializable
internal data class UrlDto(val url: String)

@Serializable
internal data class EvolutionChainDto(val chain: ChainLinkDto)

@Serializable
internal data class ChainLinkDto(
    val species: NamedResourceDto,
    @SerialName("evolution_details") val evolutionDetails: List<EvolutionDetailDto> = emptyList(),
    @SerialName("evolves_to") val evolvesTo: List<ChainLinkDto> = emptyList(),
)

@Serializable
internal data class EvolutionDetailDto(
    val trigger: NamedResourceDto,
    @SerialName("min_level") val minLevel: Int? = null,
    val item: NamedResourceDto? = null,
)

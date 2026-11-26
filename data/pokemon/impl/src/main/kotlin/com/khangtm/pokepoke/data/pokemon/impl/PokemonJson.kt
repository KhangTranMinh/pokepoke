package com.khangtm.pokepoke.data.pokemon.impl

import com.khangtm.pokepoke.data.pokemon.api.EvolutionCondition
import com.khangtm.pokepoke.data.pokemon.api.EvolutionStage
import com.khangtm.pokepoke.data.pokemon.api.EvolutionTrigger
import com.khangtm.pokepoke.data.pokemon.api.PokemonAbility
import com.khangtm.pokepoke.data.pokemon.api.PokemonStat
import com.khangtm.pokepoke.data.pokemon.api.PokemonSummary
import com.khangtm.pokepoke.data.pokemon.api.PokemonType
import com.khangtm.pokepoke.data.pokemon.api.StatType
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** `@Serializable` records of the JSON columns; the business models are not annotated. */
@Serializable
internal data class AbilityJson(val name: String, val isHidden: Boolean)

@Serializable
internal data class StatJson(val type: String, val baseValue: Int)

@Serializable
internal data class SummaryJson(val id: Int, val name: String, val imageUrl: String)

@Serializable
internal data class ConditionJson(val trigger: String, val minLevel: Int?, val item: String?)

@Serializable
internal data class StageJson(
    val pokemon: SummaryJson,
    val condition: ConditionJson?,
    val evolvesTo: List<StageJson>,
)

/** Converts the detail's collections to and from the JSON columns. */
internal class PokemonJsonCodec(private val json: Json = Json { ignoreUnknownKeys = true }) {

    private val typesSerializer = ListSerializer(String.serializer())
    private val abilitiesSerializer = ListSerializer(AbilityJson.serializer())
    private val statsSerializer = ListSerializer(StatJson.serializer())

    fun encodeTypes(types: List<PokemonType>): String =
        json.encodeToString(typesSerializer, types.map { it.key })

    fun decodeTypes(text: String): List<PokemonType> =
        json.decodeFromString(typesSerializer, text).map(PokemonType::fromKey)

    fun encodeAbilities(abilities: List<PokemonAbility>): String =
        json.encodeToString(abilitiesSerializer, abilities.map { AbilityJson(it.name, it.isHidden) })

    fun decodeAbilities(text: String): List<PokemonAbility> =
        json.decodeFromString(abilitiesSerializer, text).map { PokemonAbility(it.name, it.isHidden) }

    fun encodeStats(stats: List<PokemonStat>): String =
        json.encodeToString(statsSerializer, stats.map { StatJson(it.type.key, it.baseValue) })

    fun decodeStats(text: String): List<PokemonStat> =
        json.decodeFromString(statsSerializer, text).map { PokemonStat(StatType.fromKey(it.type), it.baseValue) }

    fun encodeEvolution(stage: EvolutionStage): String =
        json.encodeToString(StageJson.serializer(), stage.toJson())

    fun decodeEvolution(text: String): EvolutionStage =
        json.decodeFromString(StageJson.serializer(), text).toModel()

    private fun EvolutionStage.toJson(): StageJson = StageJson(
        pokemon = SummaryJson(pokemon.id, pokemon.name, pokemon.imageUrl),
        condition = condition?.let { ConditionJson(it.trigger.name, it.minLevel, it.item) },
        evolvesTo = evolvesTo.map { it.toJson() },
    )

    private fun StageJson.toModel(): EvolutionStage = EvolutionStage(
        pokemon = PokemonSummary(pokemon.id, pokemon.name, pokemon.imageUrl),
        condition = condition?.let {
            EvolutionCondition(
                trigger = EvolutionTrigger.entries.firstOrNull { trigger -> trigger.name == it.trigger }
                    ?: EvolutionTrigger.OTHER,
                minLevel = it.minLevel,
                item = it.item,
            )
        },
        evolvesTo = evolvesTo.map { it.toModel() },
    )
}

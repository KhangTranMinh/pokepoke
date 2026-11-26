package com.khangtm.pokepoke.business.domain.pokemon

data class PokemonSummary(val id: Int, val name: String, val imageUrl: String)

data class PokemonDetail(
    val summary: PokemonSummary,
    val heightDecimeters: Int,
    val weightHectograms: Int,
    val baseExperience: Int?,
    /** In slot order. */
    val types: List<PokemonType>,
    /** In slot order. */
    val abilities: List<PokemonAbility>,
    val stats: List<PokemonStat>,
    /** Root of the evolution chain. */
    val evolutionChain: EvolutionStage,
)

/** [name] is the API key, e.g. "overgrow". */
data class PokemonAbility(val name: String, val isHidden: Boolean)

data class PokemonStat(val type: StatType, val baseValue: Int)

data class EvolutionStage(
    val pokemon: PokemonSummary,
    /** Null at the root. */
    val condition: EvolutionCondition?,
    val evolvesTo: List<EvolutionStage>,
)

data class EvolutionCondition(val trigger: EvolutionTrigger, val minLevel: Int?, val item: String?)

enum class EvolutionTrigger {
    LEVEL_UP, USE_ITEM, TRADE, OTHER;

    companion object {
        /** "level-up", "use-item", "trade"; anything else is [OTHER]. */
        fun fromKey(key: String): EvolutionTrigger = when (key) {
            "level-up" -> LEVEL_UP
            "use-item" -> USE_ITEM
            "trade" -> TRADE
            else -> OTHER
        }
    }
}

enum class PokemonType(val key: String) {
    NORMAL("normal"), FIRE("fire"), WATER("water"), ELECTRIC("electric"), GRASS("grass"), ICE("ice"),
    FIGHTING("fighting"), POISON("poison"), GROUND("ground"), FLYING("flying"), PSYCHIC("psychic"),
    BUG("bug"), ROCK("rock"), GHOST("ghost"), DRAGON("dragon"), DARK("dark"), STEEL("steel"),
    FAIRY("fairy"), UNKNOWN("unknown");

    companion object {
        /** No match → [UNKNOWN]. */
        fun fromKey(key: String): PokemonType = entries.firstOrNull { it.key == key } ?: UNKNOWN
    }
}

enum class StatType(val key: String) {
    HP("hp"), ATTACK("attack"), DEFENSE("defense"), SPECIAL_ATTACK("special-attack"),
    SPECIAL_DEFENSE("special-defense"), SPEED("speed"), UNKNOWN("unknown");

    companion object {
        /** No match → [UNKNOWN]. */
        fun fromKey(key: String): StatType = entries.firstOrNull { it.key == key } ?: UNKNOWN
    }
}

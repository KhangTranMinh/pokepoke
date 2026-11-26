package com.khangtm.pokepoke.data.pokemon.api

/** Sample data for tests. */
object PokemonFixtures {

    fun summary(id: Int, name: String): PokemonSummary = PokemonSummary(id, name, PokemonImageUrls.artworkUrl(id))

    val bulbasaur = summary(1, "bulbasaur")
    val ivysaur = summary(2, "ivysaur")
    val venusaur = summary(3, "venusaur")
    val pikachu = summary(25, "pikachu")
    val raichu = summary(26, "raichu")
    val pichu = summary(172, "pichu")
    val eevee = summary(133, "eevee")
    val mrMime = summary(122, "mr-mime")

    /** Eevee's eight evolutions, in id order. */
    val eeveelutions: List<PokemonSummary> = listOf(
        summary(134, "vaporeon"),
        summary(135, "jolteon"),
        summary(136, "flareon"),
        summary(196, "espeon"),
        summary(197, "umbreon"),
        summary(470, "leafeon"),
        summary(471, "glaceon"),
        summary(700, "sylveon"),
    )

    private val defaultStats = listOf(
        PokemonStat(StatType.HP, 45),
        PokemonStat(StatType.ATTACK, 49),
        PokemonStat(StatType.DEFENSE, 49),
        PokemonStat(StatType.SPECIAL_ATTACK, 65),
        PokemonStat(StatType.SPECIAL_DEFENSE, 65),
        PokemonStat(StatType.SPEED, 45),
    )

    private fun stage(
        pokemon: PokemonSummary,
        condition: EvolutionCondition? = null,
        vararg evolvesTo: EvolutionStage,
    ) = EvolutionStage(pokemon, condition, evolvesTo.toList())

    private fun level(minLevel: Int) = EvolutionCondition(EvolutionTrigger.LEVEL_UP, minLevel, null)

    private fun item(name: String) = EvolutionCondition(EvolutionTrigger.USE_ITEM, null, name)

    val bulbasaurChain: EvolutionStage = stage(
        bulbasaur, null,
        stage(ivysaur, level(16), stage(venusaur, level(32))),
    )

    val pikachuChain: EvolutionStage = stage(
        pichu, null,
        stage(
            pikachu, EvolutionCondition(EvolutionTrigger.OTHER, null, null),
            stage(raichu, item("thunder-stone")),
        ),
    )

    val eeveeChain: EvolutionStage = EvolutionStage(
        eevee, null,
        eeveelutions.map { EvolutionStage(it, item("some-stone"), emptyList()) },
    )

    val bulbasaurDetail = PokemonDetail(
        summary = bulbasaur,
        heightDecimeters = 7,
        weightHectograms = 69,
        baseExperience = 64,
        types = listOf(PokemonType.GRASS, PokemonType.POISON),
        abilities = listOf(PokemonAbility("overgrow", false), PokemonAbility("chlorophyll", true)),
        stats = defaultStats,
        evolutionChain = bulbasaurChain,
    )

    val ivysaurDetail = bulbasaurDetail.copy(summary = ivysaur, heightDecimeters = 10, weightHectograms = 130)

    val venusaurDetail = bulbasaurDetail.copy(summary = venusaur, heightDecimeters = 20, weightHectograms = 1000)

    val pikachuDetail = PokemonDetail(
        summary = pikachu,
        heightDecimeters = 4,
        weightHectograms = 60,
        baseExperience = 112,
        types = listOf(PokemonType.ELECTRIC),
        abilities = listOf(PokemonAbility("static", false), PokemonAbility("lightning-rod", true)),
        stats = defaultStats,
        evolutionChain = pikachuChain,
    )

    /** No base experience, to cover the nullable column. */
    val eeveeDetail = PokemonDetail(
        summary = eevee,
        heightDecimeters = 3,
        weightHectograms = 65,
        baseExperience = null,
        types = listOf(PokemonType.NORMAL),
        abilities = listOf(PokemonAbility("run-away", false), PokemonAbility("adaptability", false)),
        stats = defaultStats,
        evolutionChain = eeveeChain,
    )
}

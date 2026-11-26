plugins {
    alias(libs.plugins.pokepoke.jvm.library)
}

dependencies {
    api(projects.core.commonKotlin)
    api(libs.kotlinx.coroutines.core)

    // The shared contract test (PokemonRepositoryContract) lives in testFixtures so the fake and the Room
    // implementation are checked against the same rules.
    testFixturesApi(libs.junit)
    testFixturesApi(libs.kotlinx.coroutines.test)
}

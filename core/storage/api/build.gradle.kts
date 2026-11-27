plugins {
    alias(pokepoke.plugins.pokepoke.jvm.library)
}

dependencies {
    api(projects.core.commonKotlin)
    api(pokepoke.kotlinx.coroutines.core)

    // The shared contract test (KeyValueStoreContract) lives in testFixtures so the fake and the real implementation
    // are checked against the same rules.
    testFixturesApi(pokepoke.junit)
    testFixturesApi(pokepoke.kotlinx.coroutines.test)
}

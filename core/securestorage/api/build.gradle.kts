plugins {
    alias(pokepoke.plugins.pokepoke.jvm.library)
}

dependencies {
    api(projects.core.commonKotlin)
    api(pokepoke.kotlinx.coroutines.core)

    // SecureStorageContract (testFixtures) checks the fake and the real implementation against the same rules.
    testFixturesApi(pokepoke.junit)
    testFixturesApi(pokepoke.kotlinx.coroutines.test)
}

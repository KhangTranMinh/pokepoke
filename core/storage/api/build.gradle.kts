plugins {
    alias(libs.plugins.pokepoke.jvm.library)
}

dependencies {
    api(projects.core.commonKotlin)
    api(libs.kotlinx.coroutines.core)

    // The shared contract test (KeyValueStoreContract) lives in testFixtures so the fake and the real implementation
    // are checked against the same rules.
    testFixturesApi(libs.junit)
    testFixturesApi(libs.kotlinx.coroutines.test)
}

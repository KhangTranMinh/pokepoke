plugins {
    alias(libs.plugins.pokepoke.jvm.library)
}

dependencies {
    api(projects.core.commonKotlin)
    api(libs.kotlinx.coroutines.core)

    // SecureStorageContract (testFixtures) checks the fake and the real implementation against the same rules.
    testFixturesApi(libs.junit)
    testFixturesApi(libs.kotlinx.coroutines.test)
}

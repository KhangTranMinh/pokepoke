plugins {
    alias(pokepoke.plugins.pokepoke.jvm.library)
    alias(pokepoke.plugins.ksp)
}

dependencies {
    api(projects.core.commonKotlin)
    api(pokepoke.kotlinx.coroutines.core)
    // Hilt for a pure Kotlin/JVM module: @Inject + @Module @InstallIn (build.md §3).
    implementation(pokepoke.hilt.core)
    ksp(pokepoke.hilt.compiler)

    testFixturesApi(pokepoke.junit)
    testFixturesApi(pokepoke.kotlinx.coroutines.test)
}

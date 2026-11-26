plugins {
    alias(libs.plugins.pokepoke.jvm.library)
    alias(libs.plugins.ksp)
}

dependencies {
    api(projects.core.commonKotlin)
    api(libs.kotlinx.coroutines.core)
    // Hilt for a pure Kotlin/JVM module: @Inject + @Module @InstallIn (build.md §3).
    implementation(libs.hilt.core)
    ksp(libs.hilt.compiler)

    testFixturesApi(libs.junit)
    testFixturesApi(libs.kotlinx.coroutines.test)
}

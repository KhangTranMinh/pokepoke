plugins {
    alias(libs.plugins.pokepoke.android.library)
    alias(libs.plugins.pokepoke.hilt)
    alias(libs.plugins.pokepoke.room)
    alias(libs.plugins.pokepoke.kotlin.serialization)
}

android {
    namespace = "com.khangtm.pokepoke.data.pokemon.impl"
}

dependencies {
    implementation(projects.data.pokemon.api)
    implementation(projects.core.commonKotlin)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(testFixtures(projects.data.pokemon.api))
}

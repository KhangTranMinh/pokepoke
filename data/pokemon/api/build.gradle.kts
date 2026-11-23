plugins {
    alias(libs.plugins.pokepoke.android.library)
}

android {
    namespace = "com.khangtm.pokepoke.data.pokemon.api"
    testFixtures.enable = true
}

dependencies {
    api(projects.core.common)
    api(libs.kotlinx.coroutines.core)
}

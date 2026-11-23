plugins {
    alias(libs.plugins.pokepoke.android.library)
    alias(libs.plugins.pokepoke.hilt)
    alias(libs.plugins.pokepoke.room)
}

android {
    namespace = "com.khangtm.pokepoke.data.pokemon.impl"
}

dependencies {
    implementation(projects.data.pokemon.api)
    implementation(projects.core.common.kotlin)
}

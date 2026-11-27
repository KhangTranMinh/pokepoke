plugins {
    alias(pokepoke.plugins.pokepoke.android.library)
    alias(pokepoke.plugins.pokepoke.hilt)
}

android {
    namespace = "com.khangtm.pokepoke.core.connectivity"
    testFixtures.enable = true
}

dependencies {
    api(pokepoke.kotlinx.coroutines.core)
    implementation(pokepoke.kotlinx.coroutines.android)
}

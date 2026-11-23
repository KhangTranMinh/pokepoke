plugins {
    alias(libs.plugins.pokepoke.android.library)
}

android {
    namespace = "com.khangtm.pokepoke.core.network.api"
    testFixtures.enable = true
}

dependencies {
    api(projects.core.common)
    api(libs.retrofit)
    api(libs.okhttp)
    api(libs.kotlinx.coroutines.core)
}

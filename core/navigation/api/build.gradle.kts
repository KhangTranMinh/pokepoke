plugins {
    alias(libs.plugins.pokepoke.android.library)
}

android {
    namespace = "com.khangtm.pokepoke.core.navigation.api"
    testFixtures.enable = true
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.material.icons.core)
    api(libs.kotlinx.coroutines.core)
}

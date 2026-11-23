plugins {
    alias(libs.plugins.pokepoke.android.library)
    alias(libs.plugins.pokepoke.android.compose)
}

android {
    namespace = "com.khangtm.pokepoke.core.testing"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(projects.core.common)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.roborazzi)
    api(libs.roborazzi.compose)
    api(libs.roborazzi.junit.rule)
    api(libs.robolectric)
}

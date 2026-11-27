plugins {
    alias(pokepoke.plugins.pokepoke.android.library)
    alias(pokepoke.plugins.pokepoke.android.compose)
}

android {
    namespace = "com.khangtm.pokepoke.core.testing"
}

dependencies {
    api(platform(pokepoke.androidx.compose.bom))
    api(projects.core.commonKotlin)
    api(pokepoke.junit)
    api(pokepoke.kotlinx.coroutines.test)
    api(pokepoke.androidx.compose.ui.test.junit4)
    api(pokepoke.roborazzi)
    api(pokepoke.roborazzi.compose)
    api(pokepoke.roborazzi.junit.rule)
    api(pokepoke.robolectric)
}

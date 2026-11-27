plugins {
    alias(pokepoke.plugins.pokepoke.android.library)
    alias(pokepoke.plugins.pokepoke.hilt)
}

android {
    namespace = "com.khangtm.pokepoke.core.navigation"
    testFixtures.enable = true
}

dependencies {
    api(platform(pokepoke.androidx.compose.bom))
    api(pokepoke.androidx.navigation3.runtime)
    api(pokepoke.androidx.compose.ui.graphics)
    api(pokepoke.androidx.compose.material.icons.core)
    api(pokepoke.kotlinx.coroutines.core)
    implementation(projects.core.commonKotlin)
    implementation(pokepoke.kotlinx.coroutines.android)

    // NavigatorContract (testFixtures) checks FakeNavigator and NavigatorImpl against the same rules.
    testFixturesApi(pokepoke.junit)
    testFixturesApi(pokepoke.kotlinx.coroutines.test)
}

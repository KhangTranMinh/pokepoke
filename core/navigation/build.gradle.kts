plugins {
    alias(libs.plugins.pokepoke.android.library)
    alias(libs.plugins.pokepoke.hilt)
}

android {
    namespace = "com.khangtm.pokepoke.core.navigation"
    testFixtures.enable = true
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.material.icons.core)
    api(libs.kotlinx.coroutines.core)
    implementation(projects.core.commonKotlin)
    implementation(libs.kotlinx.coroutines.android)

    // NavigatorContract (testFixtures) checks FakeNavigator and NavigatorImpl against the same rules.
    testFixturesApi(libs.junit)
    testFixturesApi(libs.kotlinx.coroutines.test)
}

plugins {
    alias(libs.plugins.pokepoke.android.library)
    alias(libs.plugins.pokepoke.hilt)
}

android {
    namespace = "com.khangtm.pokepoke.core.navigation.impl"
}

dependencies {
    implementation(projects.core.navigation.api)
    implementation(projects.core.commonKotlin)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(testFixtures(projects.core.navigation.api))
}

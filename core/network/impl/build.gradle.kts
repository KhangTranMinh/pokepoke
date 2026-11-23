plugins {
    alias(libs.plugins.pokepoke.android.library)
    alias(libs.plugins.pokepoke.hilt)
    alias(libs.plugins.pokepoke.kotlin.serialization)
}

android {
    namespace = "com.khangtm.pokepoke.core.network.impl"
}

dependencies {
    implementation(projects.core.network.api)
    implementation(projects.core.commonKotlin)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(projects.core.network.retrofit)
    testImplementation(libs.okhttp.mockwebserver)
}

plugins {
    alias(libs.plugins.pokepoke.android.library)
    alias(libs.plugins.pokepoke.hilt)
    alias(libs.plugins.pokepoke.kotlin.serialization)
}

android {
    namespace = "com.khangtm.pokepoke.core.network"
    testFixtures.enable = true
}

dependencies {
    api(projects.core.commonKotlin)
    // Features declare Retrofit services (@GET...) and :debugtool writes OkHttp interceptors.
    api(libs.retrofit)
    api(libs.okhttp)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    testFixturesImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.okhttp.mockwebserver)
}

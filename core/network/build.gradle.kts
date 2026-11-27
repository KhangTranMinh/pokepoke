plugins {
    alias(pokepoke.plugins.pokepoke.android.library)
    alias(pokepoke.plugins.pokepoke.hilt)
    alias(pokepoke.plugins.pokepoke.kotlin.serialization)
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
    testFixturesImplementation(pokepoke.kotlinx.serialization.json)
    testImplementation(libs.okhttp.mockwebserver)
}

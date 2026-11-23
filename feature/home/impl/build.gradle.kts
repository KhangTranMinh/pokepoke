plugins {
    alias(libs.plugins.pokepoke.android.feature.impl)
    alias(libs.plugins.pokepoke.kotlin.serialization)
}

android {
    namespace = "com.khangtm.pokepoke.feature.home.impl"
}

dependencies {
    implementation(projects.feature.home.api)
    implementation(projects.data.pokemon.api)
    implementation(projects.feature.details.api)
    implementation(projects.core.network.api)
    implementation(projects.core.deeplink.api)
    testImplementation(libs.okhttp.mockwebserver)
}

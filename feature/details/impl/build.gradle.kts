plugins {
    alias(pokepoke.plugins.pokepoke.android.feature.impl)
    alias(pokepoke.plugins.pokepoke.kotlin.serialization)
}

android {
    namespace = "com.khangtm.pokepoke.feature.details.impl"
}

dependencies {
    implementation(projects.feature.details.api)
    implementation(projects.business.domain)
    implementation(projects.feature.favourite.api)
    implementation(projects.feature.home.api)
    implementation(projects.core.network)
    implementation(projects.core.connectivity)
    implementation(projects.core.deeplink)
    testImplementation(libs.okhttp.mockwebserver)
}

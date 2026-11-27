plugins {
    alias(pokepoke.plugins.pokepoke.android.library)
    alias(pokepoke.plugins.pokepoke.hilt)
    alias(pokepoke.plugins.pokepoke.room)
    alias(pokepoke.plugins.pokepoke.kotlin.serialization)
}

android {
    namespace = "com.khangtm.pokepoke.business.data"
}

dependencies {
    // Repository interfaces and models; features read them through :business:domain.
    api(projects.business.domain)
    implementation(projects.core.commonKotlin)
    implementation(projects.core.network)
    implementation(projects.core.connectivity)
    implementation(pokepoke.kotlinx.coroutines.core)

    testImplementation(testFixtures(projects.business.domain))
    testImplementation(testFixtures(projects.core.network))
    testImplementation(testFixtures(projects.core.connectivity))
    testImplementation(libs.okhttp.mockwebserver)
}

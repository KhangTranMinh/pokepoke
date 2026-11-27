plugins {
    alias(pokepoke.plugins.pokepoke.android.library)
    alias(pokepoke.plugins.pokepoke.hilt)
}

android {
    namespace = "com.khangtm.pokepoke.core.securestorage.impl"
}

dependencies {
    implementation(projects.core.securestorage.api)
    implementation(projects.core.commonKotlin)
    implementation(coreLibs.androidx.datastore.preferences)
    implementation(coreLibs.tink.android)
    testImplementation(testFixtures(projects.core.securestorage.api))
}

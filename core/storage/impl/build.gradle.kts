plugins {
    alias(pokepoke.plugins.pokepoke.android.library)
    alias(pokepoke.plugins.pokepoke.hilt)
}

android {
    namespace = "com.khangtm.pokepoke.core.storage.impl"
}

dependencies {
    implementation(projects.core.storage.api)
    implementation(projects.core.commonKotlin)
    implementation(coreLibs.androidx.datastore.preferences)
    testImplementation(testFixtures(projects.core.storage.api))
}

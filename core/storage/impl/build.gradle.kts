plugins {
    alias(libs.plugins.pokepoke.android.library)
    alias(libs.plugins.pokepoke.hilt)
}

android {
    namespace = "com.khangtm.pokepoke.core.storage.impl"
}

dependencies {
    implementation(projects.core.storage.api)
    implementation(projects.core.commonKotlin)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(testFixtures(projects.core.storage.api))
}

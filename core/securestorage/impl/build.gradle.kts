plugins {
    alias(libs.plugins.pokepoke.android.library)
    alias(libs.plugins.pokepoke.hilt)
}

android {
    namespace = "com.khangtm.pokepoke.core.securestorage.impl"
}

dependencies {
    implementation(projects.core.securestorage.api)
    implementation(projects.core.commonKotlin)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.tink.android)
}

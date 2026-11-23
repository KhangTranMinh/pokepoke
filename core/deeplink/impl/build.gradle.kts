plugins {
    alias(libs.plugins.pokepoke.android.library)
    alias(libs.plugins.pokepoke.hilt)
}

android {
    namespace = "com.khangtm.pokepoke.core.deeplink.impl"
}

dependencies {
    implementation(projects.core.deeplink.api)
    implementation(projects.core.navigation.api)
    implementation(projects.core.common)
}

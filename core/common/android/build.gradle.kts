plugins {
    alias(libs.plugins.pokepoke.android.library)
    alias(libs.plugins.pokepoke.hilt)
}

android {
    namespace = "com.khangtm.pokepoke.core.common.android"
}

dependencies {
    implementation(projects.core.common.kotlin)
    implementation(libs.kotlinx.coroutines.android)
}

plugins {
    alias(libs.plugins.pokepoke.android.library)
    alias(libs.plugins.pokepoke.android.compose)
}

android {
    namespace = "com.khangtm.pokepoke.core.ui"
}

dependencies {
    api(projects.core.commonKotlin)
    implementation(projects.core.designsystem)
    implementation(libs.coil.compose)
}

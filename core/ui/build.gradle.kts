plugins {
    alias(libs.plugins.pokepoke.android.library)
    alias(libs.plugins.pokepoke.android.compose)
}

android {
    namespace = "com.khangtm.pokepoke.core.ui"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.commonKotlin)
    implementation(libs.coil.compose)
}

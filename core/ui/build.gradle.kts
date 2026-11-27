plugins {
    alias(pokepoke.plugins.pokepoke.android.library)
    alias(pokepoke.plugins.pokepoke.android.compose)
}

android {
    namespace = "com.khangtm.pokepoke.core.ui"
}

dependencies {
    api(projects.core.commonKotlin)
    implementation(projects.core.designsystem)
    implementation(projects.translation)
    implementation(libs.coil.compose)
}

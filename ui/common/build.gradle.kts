plugins {
    alias(pokepoke.plugins.pokepoke.android.library)
    alias(pokepoke.plugins.pokepoke.android.compose)
}

android {
    namespace = "com.khangtm.pokepoke.ui.common"
}

dependencies {
    api(projects.core.commonKotlin)
    implementation(projects.ui.designsystem)
    implementation(projects.ui.translation)
    implementation(libs.coil.compose)
}

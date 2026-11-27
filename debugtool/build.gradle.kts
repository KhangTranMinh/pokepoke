plugins {
    alias(pokepoke.plugins.pokepoke.android.library)
    alias(pokepoke.plugins.pokepoke.android.compose)
    alias(pokepoke.plugins.pokepoke.hilt)
}

android {
    namespace = "com.khangtm.pokepoke.debugtool"
}

dependencies {
    implementation(projects.ui.designsystem)
    implementation(projects.ui.common)
    implementation(projects.core.commonKotlin)
    implementation(projects.core.network)
    implementation(projects.core.storage.api)
    implementation(projects.core.securestorage.api)
    implementation(libs.androidx.startup)
    implementation(libs.androidx.activity.compose)
    implementation(pokepoke.androidx.lifecycle.runtime.compose)
    implementation(pokepoke.androidx.lifecycle.viewmodel.compose)
    implementation(pokepoke.androidx.hilt.lifecycle.viewmodel.compose)
}

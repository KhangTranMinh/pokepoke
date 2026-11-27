plugins {
    alias(pokepoke.plugins.pokepoke.android.library)
    alias(pokepoke.plugins.pokepoke.android.compose)
}

android {
    namespace = "com.khangtm.pokepoke.ui.designsystem"
}

dependencies {
    implementation(projects.ui.translation)
}

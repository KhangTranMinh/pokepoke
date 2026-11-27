plugins {
    alias(pokepoke.plugins.pokepoke.android.library)
    alias(pokepoke.plugins.pokepoke.android.compose)
}

android {
    namespace = "com.khangtm.pokepoke.core.designsystem"
}

dependencies {
    implementation(projects.translation)
}

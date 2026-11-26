plugins {
    alias(libs.plugins.pokepoke.android.feature.impl)
}

android {
    namespace = "com.khangtm.pokepoke.feature.main.impl"
}

dependencies {
    implementation(projects.feature.main.api)
    implementation(libs.androidx.compose.material3.navigation.suite)
}

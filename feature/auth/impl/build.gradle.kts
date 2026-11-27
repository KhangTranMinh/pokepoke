plugins {
    alias(pokepoke.plugins.pokepoke.android.feature.impl)
}

android {
    namespace = "com.khangtm.pokepoke.feature.auth.impl"
}

dependencies {
    implementation(projects.feature.auth.api)
    implementation(projects.core.securestorage.api)
}

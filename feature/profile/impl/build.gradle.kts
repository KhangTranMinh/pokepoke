plugins {
    alias(pokepoke.plugins.pokepoke.android.feature.impl)
}

android {
    namespace = "com.khangtm.pokepoke.feature.profile.impl"
}

dependencies {
    implementation(projects.feature.profile.api)
    implementation(projects.feature.auth.api)
    implementation(projects.core.deeplink)
}

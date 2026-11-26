plugins {
    alias(libs.plugins.pokepoke.android.feature.impl)
    alias(libs.plugins.pokepoke.room)
}

android {
    namespace = "com.khangtm.pokepoke.feature.favourite.impl"
}

dependencies {
    implementation(projects.feature.favourite.api)
    implementation(projects.business.domain)
    implementation(projects.feature.details.api)
    implementation(projects.feature.auth.api)
    implementation(projects.core.deeplink)
}

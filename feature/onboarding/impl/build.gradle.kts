plugins {
    alias(libs.plugins.pokepoke.android.feature.impl)
}

android {
    namespace = "com.khangtm.pokepoke.feature.onboarding.impl"
}

dependencies {
    implementation(projects.feature.onboarding.api)
    implementation(projects.core.storage.api)
    testImplementation(testFixtures(projects.core.storage.api))
}

plugins {
    alias(libs.plugins.pokepoke.android.application)
    alias(libs.plugins.pokepoke.android.compose)
    alias(libs.plugins.pokepoke.hilt)
}

android {
    namespace = "com.khangtm.pokepoke"

    defaultConfig {
        applicationId = "com.khangtm.pokepoke"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(projects.core.commonKotlin)
    implementation(projects.translation)
    implementation(projects.core.commonAndroid)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.network)
    implementation(projects.core.connectivity)
    implementation(projects.core.storage.api)
    implementation(projects.core.storage.impl)
    implementation(projects.core.securestorage.api)
    implementation(projects.core.securestorage.impl)
    implementation(projects.core.navigation)
    implementation(projects.core.deeplink)
    implementation(projects.business.domain)
    implementation(projects.business.data)
    implementation(projects.feature.onboarding.api)
    implementation(projects.feature.onboarding.impl)
    implementation(projects.feature.auth.api)
    implementation(projects.feature.auth.impl)
    implementation(projects.feature.main.api)
    implementation(projects.feature.main.impl)
    implementation(projects.feature.home.api)
    implementation(projects.feature.home.impl)
    implementation(projects.feature.details.api)
    implementation(projects.feature.details.impl)
    implementation(projects.feature.favourite.api)
    implementation(projects.feature.favourite.impl)
    implementation(projects.feature.profile.api)
    implementation(projects.feature.profile.impl)
    // The debug tool belongs to the dev / qa environments, never to prod (docs/design/build.md §4a, §5).
    "devImplementation"(projects.debugtool)
    "qaImplementation"(projects.debugtool)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(projects.core.testing)
    testImplementation(testFixtures(projects.core.navigation))
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.espresso.core)
}

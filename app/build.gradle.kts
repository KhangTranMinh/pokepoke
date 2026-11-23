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
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.espresso.core)
}

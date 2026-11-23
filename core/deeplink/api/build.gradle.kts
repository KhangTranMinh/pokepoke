plugins {
    alias(libs.plugins.pokepoke.android.library)
}

android {
    namespace = "com.khangtm.pokepoke.core.deeplink.api"
    testFixtures.enable = true
}

dependencies {
    api(projects.core.navigation.api)
    api(projects.core.commonKotlin)
}

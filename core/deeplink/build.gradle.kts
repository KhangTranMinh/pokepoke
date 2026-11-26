plugins {
    alias(libs.plugins.pokepoke.android.library)
    alias(libs.plugins.pokepoke.hilt)
}

android {
    namespace = "com.khangtm.pokepoke.core.deeplink"
    testFixtures.enable = true
}

dependencies {
    // NavRequest is part of the DeepLinkHandler signature.
    api(projects.core.navigation)
}

dependencies {
    testImplementation(testFixtures(projects.core.navigation))
}

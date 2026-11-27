plugins {
    alias(pokepoke.plugins.pokepoke.android.library)
    alias(pokepoke.plugins.pokepoke.hilt)
}

android {
    namespace = "com.khangtm.pokepoke.core.common.android"
}

dependencies {
    implementation(projects.core.commonKotlin)
    implementation(pokepoke.kotlinx.coroutines.android)
}

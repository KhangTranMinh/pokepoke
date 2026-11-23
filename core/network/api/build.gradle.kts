plugins {
    alias(libs.plugins.pokepoke.jvm.library)
}

dependencies {
    api(projects.core.common.kotlin)
    api(libs.retrofit)
    api(libs.okhttp)
    api(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)
}

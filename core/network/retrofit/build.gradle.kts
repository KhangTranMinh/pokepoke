plugins {
    alias(libs.plugins.pokepoke.jvm.library)
}

dependencies {
    api(projects.core.commonKotlin)
    api(libs.retrofit)
    implementation(libs.kotlinx.serialization.json)
}

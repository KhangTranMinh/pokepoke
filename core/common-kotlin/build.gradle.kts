plugins {
    alias(libs.plugins.pokepoke.jvm.library)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)
}

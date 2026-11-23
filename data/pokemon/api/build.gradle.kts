plugins {
    alias(libs.plugins.pokepoke.jvm.library)
}

dependencies {
    api(projects.core.common.kotlin)
    api(libs.kotlinx.coroutines.core)
}

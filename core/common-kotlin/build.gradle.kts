plugins {
    alias(pokepoke.plugins.pokepoke.jvm.library)
}

dependencies {
    api(pokepoke.kotlinx.coroutines.core)
    api(libs.javax.inject)
}

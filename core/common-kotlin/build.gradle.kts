plugins {
    alias(pokepoke.plugins.pokepoke.jvm.library)
}

dependencies {
    api(pokepoke.kotlinx.coroutines.core)
    api(coreLibs.javax.inject)
}

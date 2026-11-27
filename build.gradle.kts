// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(pokepoke.plugins.android.application) apply false
    alias(pokepoke.plugins.android.library) apply false
    alias(pokepoke.plugins.kotlin.jvm) apply false
    alias(pokepoke.plugins.kotlin.compose) apply false
    alias(pokepoke.plugins.kotlin.serialization) apply false
    alias(pokepoke.plugins.ksp) apply false
    alias(pokepoke.plugins.hilt) apply false
    alias(pokepoke.plugins.room) apply false
    alias(pokepoke.plugins.kover) apply false
    alias(pokepoke.plugins.roborazzi) apply false
}

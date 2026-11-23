package com.khangtm.pokepoke.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinSerializationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        addDep("implementation", "kotlinx-serialization-json")
    }
}

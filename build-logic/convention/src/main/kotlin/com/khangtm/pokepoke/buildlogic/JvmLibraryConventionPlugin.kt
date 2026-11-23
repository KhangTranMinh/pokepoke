package com.khangtm.pokepoke.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("pokepoke.kover")

        configureJvmToolchain()
        registerVerifyModuleDependencies()

        addDep("testImplementation", "junit")
        addDep("testImplementation", "kotlinx-coroutines-test")
        addDep("testImplementation", "turbine")
        addDep("testImplementation", "mockk")
    }
}

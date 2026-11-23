package com.khangtm.pokepoke.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("java-test-fixtures")
        pluginManager.apply("pokepoke.kover")

        configureJvmToolchain()
        registerVerifyModuleDependencies()
        tasks.withType<Test>().configureEach { failOnNoDiscoveredTests.set(false) }

        addDep("testImplementation", "junit")
        addDep("testImplementation", "kotlinx-coroutines-test")
        addDep("testImplementation", "turbine")
        addDep("testImplementation", "mockk")
    }
}

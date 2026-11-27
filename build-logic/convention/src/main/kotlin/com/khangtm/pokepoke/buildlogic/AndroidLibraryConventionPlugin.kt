package com.khangtm.pokepoke.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.api.tasks.testing.Test

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("pokepoke.kover")

            extensions.configure<LibraryExtension> {
                configureAndroidCommon(this)
                testOptions.unitTests.isIncludeAndroidResources = true
            }
            registerVerifyModuleDependencies()
            tasks.withType<Test>().configureEach { failOnNoDiscoveredTests.set(false) }

            addDep("testImplementation", "junit")
            addDep("testImplementation", "mockk")
            addDep("testImplementation", "turbine")
            addDep("testImplementation", "kotlinx-coroutines-test")
            addDep("testImplementation", "robolectric")
            addDep("testImplementation", "androidx-test-junit")
            // Which modules are added, and which modules are excluded (the test toolkit itself, resource-only
            // modules), is defined in configs/build/structure.toml [library].
            val structure = moduleStructure()
            if (structure.receivesLibraryAutoDependencies(path)) {
                structure.libraryTestImplementation.filter { it != path }
                    .forEach { dependencies.add("testImplementation", project(it)) }
            }
        }
    }
}

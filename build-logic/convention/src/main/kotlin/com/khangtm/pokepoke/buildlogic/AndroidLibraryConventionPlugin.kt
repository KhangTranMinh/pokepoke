package com.khangtm.pokepoke.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

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

            addDep("testImplementation", "junit")
            addDep("testImplementation", "mockk")
            addDep("testImplementation", "turbine")
            addDep("testImplementation", "kotlinx-coroutines-test")
            addDep("testImplementation", "robolectric")
            addDep("testImplementation", "androidx-test-junit")
            if (path != ":core:testing") {
                dependencies.add("testImplementation", project(":core:testing"))
            }
        }
    }
}

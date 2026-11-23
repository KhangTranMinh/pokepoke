package com.khangtm.pokepoke.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidFeatureApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("pokepoke.android.library")

            extensions.configure<LibraryExtension> {
                testFixtures.enable = true
            }
            dependencies.add("api", project(":core:common-kotlin"))
            dependencies.add("api", project(":core:navigation:api"))
            addDep("implementation", "kotlinx-coroutines-core")
        }
    }
}

package com.khangtm.pokepoke.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidFeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("pokepoke.android.library")
            pluginManager.apply("pokepoke.android.compose")
            pluginManager.apply("pokepoke.hilt")

            val structure = moduleStructure()
            structure.featureImplImplementation.forEach { dependencies.add("implementation", project(it)) }
            listOf(
                "androidx-lifecycle-runtime-compose",
                "androidx-lifecycle-viewmodel-compose",
                "androidx-hilt-lifecycle-viewmodel-compose",
                "androidx-navigation3-runtime",
                "androidx-compose-material3-adaptive",
                "androidx-compose-material3-adaptive-layout",
                "androidx-compose-material3-adaptive-navigation3",
            ).forEach { addDep("implementation", it) }
            structure.featureImplTestFixtures.forEach {
                dependencies.add("testImplementation", dependencies.testFixtures(project(it)))
            }
        }
    }
}

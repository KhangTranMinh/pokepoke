package com.khangtm.pokepoke.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.google.dagger.hilt.android")

        addDep("implementation", "hilt-android")
        addDep("ksp", "hilt-compiler")
        addDep("testImplementation", "hilt-android-testing")
        addDep("kspTest", "hilt-compiler")
    }
}

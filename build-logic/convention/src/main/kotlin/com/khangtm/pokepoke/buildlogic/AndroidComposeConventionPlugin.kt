package com.khangtm.pokepoke.buildlogic

import com.android.build.api.dsl.CommonExtension
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

/** Apply after `com.android.application` / `com.android.library`. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("io.github.takahirom.roborazzi")

        extensions.getByType<CommonExtension>().buildFeatures.compose = true

        val bom = libs.findLibrary("androidx-compose-bom").get()
        dependencies.add("implementation", dependencies.platform(bom))
        dependencies.add("testImplementation", dependencies.platform(bom))
        dependencies.add("androidTestImplementation", dependencies.platform(bom))

        listOf(
            "androidx-compose-ui",
            "androidx-compose-ui-graphics",
            "androidx-compose-foundation",
            "androidx-compose-material3",
            "androidx-compose-material-icons-core",
            "androidx-compose-ui-tooling-preview",
        ).forEach { addDep("implementation", it) }
        addDep("debugImplementation", "androidx-compose-ui-tooling")
        addDep("debugImplementation", "androidx-compose-ui-test-manifest")
        addDep("testImplementation", "androidx-compose-ui-test-junit4")
        addDep("testImplementation", "roborazzi")
        addDep("testImplementation", "roborazzi-compose")
        addDep("testImplementation", "roborazzi-junit-rule")

        extensions.getByType<RoborazziExtension>().outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
    }
}

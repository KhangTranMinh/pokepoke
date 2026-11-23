package com.khangtm.pokepoke.buildlogic

import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("androidx.room")
        pluginManager.apply("com.google.devtools.ksp")

        extensions.getByType<RoomExtension>().schemaDirectory("$projectDir/schemas")

        addDep("implementation", "androidx-room-runtime")
        addDep("implementation", "androidx-room-ktx")
        addDep("ksp", "androidx-room-compiler")
        addDep("testImplementation", "androidx-room-testing")
    }
}

package com.khangtm.pokepoke.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.util.Properties

internal data class AppVersion(val name: String, val code: Int)

/**
 * Reads `version.properties` at the root. The file is read through `providers.fileContents`,
 * so Gradle tracks it as a configuration input.
 */
internal fun Project.readAppVersion(): AppVersion {
    val file = rootProject.layout.projectDirectory.file("version.properties")
    val text = providers.fileContents(file).asText.orNull
        ?: throw GradleException("Missing version.properties at ${file.asFile}")
    val props = Properties().apply { load(text.reader()) }
    fun value(key: String): String = props.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw GradleException("version.properties: missing key '$key'")
    fun number(key: String): Int = value(key).toIntOrNull()
        ?: throw GradleException("version.properties: '$key' must be an integer, got '${value(key)}'")
    return AppVersion(
        name = "${number("versionMajor")}.${number("versionMinor")}.${number("versionPatch")}",
        code = number("versionCode"),
    )
}

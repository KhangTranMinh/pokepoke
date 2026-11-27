package com.khangtm.pokepoke.buildlogic

import java.io.File

/** The repo's real `configs/build/structure.toml` and the project list of the real `settings.gradle.kts`. */
internal object RealStructure {
    // Gradle runs unit tests with the working directory = build-logic/convention.
    private val root: File = File("../..").canonicalFile

    val text: String = File(root, STRUCTURE_FILE).readText()

    /** Every `":a:b"` literal of `include(...)` in settings, plus the implicit parents (`:core`, `:feature:home`...). */
    val projects: Set<String> = Regex("\"(:[^\"]+)\"").findAll(File(root, "settings.gradle.kts").readText())
        .map { it.groupValues[1] }
        .flatMap { path ->
            val parts = path.split(':').filter { it.isNotEmpty() }
            (1..parts.size).map { n -> ":" + parts.take(n).joinToString(":") }
        }
        .toSet()

    val structure: ModuleStructure = parseModuleStructure(text, projects)
}

package com.khangtm.pokepoke.buildlogic

import org.gradle.api.Project
import org.tomlj.Toml
import org.tomlj.TomlArray
import org.tomlj.TomlTable
import java.io.Serializable

/**
 * Module structure read from `configs/build/structure.toml` (docs/design/build.md §3a).
 *
 * Everything in this file except [Project.moduleStructure] is a pure function (text in, value out, no Gradle),
 * so it is unit tested without a build. Plugins receive a [ModuleStructure] instead of hardcoded project paths.
 */

internal const val STRUCTURE_FILE = "configs/build/structure.toml"

/** One `[[rules]]` entry: a module matching [from] (and not [except]) must not depend on a module matching [to]. */
data class ArchitectureRule(
    val from: List<String>,
    val except: List<String>,
    val to: List<String>,
    val reason: String,
) : Serializable

internal data class ModuleStructure(
    val libraryTestImplementation: List<String>,
    val libraryExcludeAutoDependencies: List<String>,
    val featureApiApi: List<String>,
    val featureImplImplementation: List<String>,
    val featureImplTestFixtures: List<String>,
    val appDebugTool: String,
    val koverMinLineCoverage: Int,
    val koverModules: List<String>,
    val rules: List<ArchitectureRule>,
) {
    /** Whether `pokepoke.android.library` adds [libraryTestImplementation] to the module at [path]. */
    fun receivesLibraryAutoDependencies(path: String): Boolean =
        libraryExcludeAutoDependencies.none { matches(it, path) }

    /** Whether the module at [path] must reach [koverMinLineCoverage] (Kover verify rule). */
    fun requiresCoverageThreshold(path: String): Boolean = koverModules.any { matches(it, path) }
}

internal class ModuleStructureException(message: String) : RuntimeException(message)

private fun structureFail(message: String): Nothing = throw ModuleStructureException(message)

private fun segmentsOf(value: String): List<String> = value.split(':').filter { it.isNotEmpty() }

/**
 * Matches a project [path] against a [pattern], segment by segment (segments are separated by `:`).
 * `*` matches exactly one segment, `**` matches one or more, anything else must be equal.
 * No wildcard at all means an exact match.
 */
internal fun matches(pattern: String, path: String): Boolean =
    matchSegments(segmentsOf(pattern), 0, segmentsOf(path), 0)

private fun matchSegments(pattern: List<String>, pi: Int, path: List<String>, si: Int): Boolean {
    if (pi == pattern.size) return si == path.size
    return when (pattern[pi]) {
        "**" -> (si + 1..path.size).any { matchSegments(pattern, pi + 1, path, it) }
        "*" -> si < path.size && matchSegments(pattern, pi + 1, path, si + 1)
        else -> si < path.size && pattern[pi] == path[si] && matchSegments(pattern, pi + 1, path, si + 1)
    }
}

private fun hasWildcard(value: String) = segmentsOf(value).any { it == "*" || it == "**" }

private val SECTIONS = mapOf(
    "library" to setOf("testImplementation", "excludeAutoDependencies"),
    "feature" to setOf("api", "impl"),
    "app" to setOf("debugTool"),
    "kover" to setOf("minLineCoverage", "modules"),
)
private val RULE_KEYS = setOf("from", "except", "to", "reason")

/**
 * Parses and validates [text]. [projects] is every project path of the build (without the root project);
 * a path without wildcard must be one of them (catches typos), a pattern with wildcard must match at least one
 * (no dead rule).
 */
internal fun parseModuleStructure(text: String, projects: Set<String>): ModuleStructure {
    val file = STRUCTURE_FILE
    val toml = Toml.parse(text)
    if (toml.hasErrors()) {
        val first = toml.errors().first()
        structureFail("$file: TOML syntax error: ${first.message} (${first.position()}).")
    }
    val allowedTop = SECTIONS.keys + "rules"
    for (key in toml.keySet()) {
        if (key !in allowedTop) {
            structureFail("$file: unknown top-level key '$key'; allowed: ${allowedTop.joinToString { "'$it'" }}.")
        }
    }

    fun table(name: String, parent: TomlTable = toml, label: String = name): TomlTable {
        val raw = parent.get(name) ?: structureFail("$file: missing section [$label].")
        return raw as? TomlTable ?: structureFail("$file: [$label] must be a table.")
    }

    fun checkKeys(table: TomlTable, label: String, allowed: Set<String>, required: Set<String> = allowed) {
        for (key in table.keySet()) {
            if (key !in allowed) structureFail("$file [$label]: unknown key '$key'; allowed: ${allowed.joinToString { "'$it'" }}.")
        }
        for (key in required) {
            if (table.get(key) == null) structureFail("$file [$label]: missing key '$key'.")
        }
    }

    fun strings(table: TomlTable, label: String, key: String): List<String> {
        val array = table.get(key) as? TomlArray ?: structureFail("$file [$label]: '$key' must be an array of strings.")
        return (0 until array.size()).map { i ->
            val value = array.get(i) as? String ?: structureFail("$file [$label]: '$key[$i]' must be a string.")
            if (value.isBlank()) structureFail("$file [$label]: '$key[$i]' must not be blank.")
            value
        }
    }

    /** Checks syntax and existence of one path / pattern. [wildcard] false means only an exact project path is accepted. */
    fun check(label: String, key: String, value: String, wildcard: Boolean) {
        val where = "$file [$label] $key = '$value'"
        if (value != "**" && !value.startsWith(":")) structureFail("$where: a project path starts with ':' (or is '**').")
        val segments = value.split(':').drop(if (value.startsWith(":")) 1 else 0)
        if (segments.any { it.isEmpty() }) structureFail("$where: empty path segment.")
        for (segment in segments) {
            if (segment.contains('*') && segment != "*" && segment != "**") {
                structureFail("$where: segment '$segment' is invalid; a wildcard segment must be exactly '*' or '**'.")
            }
        }
        if (hasWildcard(value)) {
            if (!wildcard) structureFail("$where: wildcards are not allowed here, use an exact project path.")
            if (projects.none { matches(value, it) }) structureFail("$where: the pattern matches no project in settings.gradle.kts.")
        } else if (value !in projects) {
            structureFail("$where: no such project in settings.gradle.kts (typo?).")
        }
    }

    fun paths(table: TomlTable, label: String, key: String, wildcard: Boolean): List<String> =
        strings(table, label, key).onEach { check(label, key, it, wildcard) }

    val library = table("library").also { checkKeys(it, "library", SECTIONS.getValue("library")) }
    val feature = table("feature").also { checkKeys(it, "feature", SECTIONS.getValue("feature")) }
    val featureApi = table("api", feature, "feature.api").also { checkKeys(it, "feature.api", setOf("api")) }
    val featureImpl = table("impl", feature, "feature.impl").also {
        checkKeys(it, "feature.impl", setOf("implementation", "testFixtures"))
    }
    val app = table("app").also { checkKeys(it, "app", SECTIONS.getValue("app")) }
    val kover = table("kover").also { checkKeys(it, "kover", SECTIONS.getValue("kover")) }

    val debugTool = app.get("debugTool") as? String ?: structureFail("$file [app]: 'debugTool' must be a string.")
    check("app", "debugTool", debugTool, wildcard = false)

    val minLine = kover.get("minLineCoverage") as? Long
        ?: structureFail("$file [kover]: 'minLineCoverage' must be an integer.")
    if (minLine !in 0..100) structureFail("$file [kover]: 'minLineCoverage' = $minLine is out of range 0..100.")

    val rules = toml.get("rules")?.let { raw ->
        val array = raw as? TomlArray ?: structureFail("$file: 'rules' must be an array of tables ([[rules]]).")
        (0 until array.size()).map { i ->
            val label = "rules #${i + 1}"
            val rule = array.get(i) as? TomlTable ?: structureFail("$file: rules[$i] must be a table.")
            checkKeys(rule, label, RULE_KEYS, required = setOf("from", "to", "reason"))
            val reason = rule.get("reason") as? String ?: structureFail("$file [$label]: 'reason' must be a string.")
            if (reason.isBlank()) structureFail("$file [$label]: 'reason' must not be blank.")
            ArchitectureRule(
                from = paths(rule, label, "from", wildcard = true),
                except = if (rule.get("except") == null) emptyList() else paths(rule, label, "except", wildcard = true),
                to = paths(rule, label, "to", wildcard = true),
                reason = reason,
            )
        }
    }.orEmpty()

    return ModuleStructure(
        libraryTestImplementation = paths(library, "library", "testImplementation", wildcard = false),
        libraryExcludeAutoDependencies = paths(library, "library", "excludeAutoDependencies", wildcard = true),
        featureApiApi = paths(featureApi, "feature.api", "api", wildcard = false),
        featureImplImplementation = paths(featureImpl, "feature.impl", "implementation", wildcard = false),
        featureImplTestFixtures = paths(featureImpl, "feature.impl", "testFixtures", wildcard = false),
        appDebugTool = debugTool,
        koverMinLineCoverage = minLine.toInt(),
        koverModules = paths(kover, "kover", "modules", wildcard = true),
        rules = rules,
    )
}

private const val STRUCTURE_EXTRA_KEY = "pokepoke.moduleStructure"

/**
 * The structure of the whole build, parsed once and shared by every module.
 *
 * Gradle lifecycle: this runs in phase 2 (configuration), the first time any convention plugin is applied to
 * any module. By then settings are evaluated, so every project already exists and `allprojects` gives the full
 * project list needed to validate the file. The file is read via `providers.fileContents` from the root project,
 * so it is a configuration cache input: editing it invalidates the cache. The parsed result is cached on the root
 * project, so later modules do not parse again. An invalid file fails the build at this point (every Gradle command
 * that configures a module), because a wrong structure makes every module wrong.
 */
internal fun Project.moduleStructure(): ModuleStructure = synchronized(rootProject) {
    val extra = rootProject.extensions.extraProperties
    if (extra.has(STRUCTURE_EXTRA_KEY)) return@synchronized extra.get(STRUCTURE_EXTRA_KEY) as ModuleStructure
    val text = providers.fileContents(rootProject.layout.projectDirectory.file(STRUCTURE_FILE)).asText.get()
    val projects = rootProject.allprojects.map { it.path }.filter { it != rootProject.path }.toSet()
    val structure = try {
        parseModuleStructure(text, projects)
    } catch (e: ModuleStructureException) {
        throw org.gradle.api.GradleException(e.message ?: "Invalid $STRUCTURE_FILE", e)
    }
    extra.set(STRUCTURE_EXTRA_KEY, structure)
    structure
}

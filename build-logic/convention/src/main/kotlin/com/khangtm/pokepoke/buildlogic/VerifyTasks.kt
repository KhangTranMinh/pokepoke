package com.khangtm.pokepoke.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Cheap check, nothing to cache")
abstract class VerifyModuleDependenciesTask : DefaultTask() {
    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val declaredProjectDependencies: ListProperty<String>

    /** `[[rules]]` of configs/build/structure.toml. */
    @get:Input
    abstract val rules: ListProperty<ArchitectureRule>

    @TaskAction
    fun verify() {
        val violations = ModuleDependencyRules.violations(rules.get(), modulePath.get(), declaredProjectDependencies.get())
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Architecture violation(s):\n" + violations.joinToString("\n") { " - $it" } +
                    "\nSee docs/design/build.md §5.",
            )
        }
    }
}

@DisableCachingByDefault(because = "Cheap check, nothing to cache")
abstract class VerifyReleaseHasNoDebugToolTask : DefaultTask() {
    @get:Input
    abstract val releaseProjectPaths: SetProperty<String>

    /** `[app].debugTool` of configs/build/structure.toml. */
    @get:Input
    abstract val debugToolPath: Property<String>

    @TaskAction
    fun verify() {
        val debugTool = debugToolPath.get()
        if (debugTool in releaseProjectPaths.get()) {
            throw GradleException(
                "A prod variant (prodDebug / prodRelease) runtime classpath contains $debugTool. " +
                    "Declare it with devImplementation / qaImplementation only (docs/design/build.md §4a, §5).",
            )
        }
    }
}

/** Registers `verifyModuleDependencies` and hooks it into `preBuild` and `check`. */
internal fun Project.registerVerifyModuleDependencies() {
    val projectPath = path // not `path` inside the lambda below: there it is the task's path
    val structure = moduleStructure()
    val verify = tasks.register<VerifyModuleDependenciesTask>("verifyModuleDependencies") {
        group = "verification"
        description = "Fails when the module breaks the architecture dependency rules."
        modulePath.set(projectPath)
        rules.set(structure.rules)
    }
    afterEvaluate {
        // Collected eagerly into plain strings so the task stays configuration-cache friendly.
        val declared = configurations
            .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java) }
            .map { it.path }
            .distinct()
        verify.configure { declaredProjectDependencies.set(declared) }
    }
    tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(verify) }
    tasks.matching { it.name == "check" }.configureEach { dependsOn(verify) }
}

/** Registers `verifyReleaseHasNoDebugTool` for the application module. */
internal fun Project.registerVerifyReleaseHasNoDebugTool() {
    val debugTool = moduleStructure().appDebugTool
    val verify = tasks.register<VerifyReleaseHasNoDebugToolTask>("verifyReleaseHasNoDebugTool") {
        group = "verification"
        description = "Fails when the debug tool module ($debugTool) is on the prodDebug or prodRelease runtime classpath."
        debugToolPath.set(debugTool)
    }
    afterEvaluate {
        // The prod flavor never has :debugtool (dev / qa have it on purpose), in either build type.
        fun root(name: String) = configurations.getByName(name).incoming.resolutionResult.rootComponent
        val paths = root("prodDebugRuntimeClasspath").zip(root("prodReleaseRuntimeClasspath")) { debug, release ->
            collectProjectPaths(debug) + collectProjectPaths(release)
        }
        verify.configure { releaseProjectPaths.set(paths) }
    }
    val prodTasks = setOf("check", "assembleProdDebug", "assembleProdRelease", "bundleProdDebug", "bundleProdRelease")
    tasks.matching { it.name in prodTasks }.configureEach { dependsOn(verify) }
}

private fun collectProjectPaths(root: ResolvedComponentResult): Set<String> {
    val visited = mutableSetOf<ResolvedComponentResult>()
    val paths = mutableSetOf<String>()
    val queue = ArrayDeque<ResolvedComponentResult>().apply { add(root) }
    while (queue.isNotEmpty()) {
        val component = queue.removeFirst()
        if (!visited.add(component)) continue
        (component.id as? ProjectComponentIdentifier)?.let { paths += it.projectPath }
        component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { queue.add(it.selected) }
    }
    return paths
}

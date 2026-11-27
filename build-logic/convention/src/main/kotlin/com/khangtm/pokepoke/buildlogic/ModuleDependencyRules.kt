package com.khangtm.pokepoke.buildlogic

/**
 * Architecture rules (docs/design/build.md §5). The rules themselves live in `[[rules]]` of
 * `configs/build/structure.toml`; this object only applies them. Free of Gradle types so it can be unit tested.
 */
internal object ModuleDependencyRules {

    /** Returns one human readable message (including the rule's `reason`) per forbidden dependency. */
    fun violations(rules: List<ArchitectureRule>, modulePath: String, dependencies: Collection<String>): List<String> {
        val applicable = rules.filter { rule ->
            rule.from.any { matches(it, modulePath) } && rule.except.none { matches(it, modulePath) }
        }
        val messages = mutableListOf<String>()
        for (dependency in dependencies.distinct().sorted()) {
            if (dependency == modulePath) continue
            val rule = applicable.firstOrNull { r -> r.to.any { matches(it, dependency) } } ?: continue
            messages += "Module '$modulePath' must not depend on '$dependency': ${rule.reason}"
        }
        return messages
    }
}

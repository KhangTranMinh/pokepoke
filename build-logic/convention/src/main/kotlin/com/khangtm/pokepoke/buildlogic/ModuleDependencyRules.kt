package com.khangtm.pokepoke.buildlogic

/** Architecture rules (docs/design/build.md §5), kept free of Gradle types so they can be unit tested. */
internal object ModuleDependencyRules {

    private fun isImpl(path: String) = path.endsWith(":impl") || path.contains(":impl:")
    private fun isDebugTool(path: String) = path == ":debugtool" || path.startsWith(":debugtool:")
    private fun inGroup(path: String, group: String) = path.startsWith(":$group:")

    /** Returns one human readable message per forbidden dependency. */
    fun violations(modulePath: String, dependencies: Collection<String>): List<String> {
        val messages = mutableListOf<String>()
        for (dependency in dependencies.distinct().sorted()) {
            if (dependency == modulePath) continue
            val reason = reasonForbidden(modulePath, dependency) ?: continue
            messages += "Module '$modulePath' must not depend on '$dependency': $reason"
        }
        return messages
    }

    private fun reasonForbidden(module: String, dependency: String): String? {
        if (module != ":app") {
            if (isImpl(dependency)) return "only :app may depend on :impl modules"
            if (isDebugTool(dependency)) return "only :app may depend on :debugtool"
        }
        if (inGroup(module, "core") && (inGroup(dependency, "data") || inGroup(dependency, "feature"))) {
            return ":core modules must not depend on :data or :feature modules"
        }
        if (inGroup(module, "data") && inGroup(dependency, "feature")) {
            return ":data modules must not depend on :feature modules"
        }
        if (isDebugTool(module) && (inGroup(dependency, "data") || inGroup(dependency, "feature"))) {
            return ":debugtool must not depend on :data or :feature modules"
        }
        return null
    }
}

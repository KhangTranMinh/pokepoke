package com.khangtm.pokepoke.buildlogic

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class KoverConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlinx.kover")

        extensions.configure<KoverProjectExtension> {
            reports {
                filters {
                    excludes {
                        classes(
                            "*_Factory", "*_Factory$*", "*_MembersInjector",
                            "*_HiltModules*", "Hilt_*", "*_Impl", "*_Impl$*",
                            "*ComposableSingletons*", "*.BuildConfig",
                            "*Module", "*Module$*", "*RouteKt", "*PreviewKt", "*.di.*",
                        )
                    }
                }
                if (requiresCoverageThreshold(path)) {
                    verify {
                        rule {
                            minBound(MIN_LINE_COVERAGE_PERCENT)
                        }
                    }
                }
            }
        }
    }

    internal companion object {
        const val MIN_LINE_COVERAGE_PERCENT = 70

        /** `:core:common`, `:core:*:impl`, `:data:*:impl`, `:feature:*:impl`, `:debugtool`. */
        fun requiresCoverageThreshold(path: String): Boolean {
            if (path == ":core:common" || path == ":debugtool") return true
            val inLayer = listOf(":core:", ":data:", ":feature:").any { path.startsWith(it) }
            return inLayer && path.endsWith(":impl")
        }
    }
}

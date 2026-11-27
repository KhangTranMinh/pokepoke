package com.khangtm.pokepoke.buildlogic

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class KoverConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlinx.kover")

        // Threshold and the list of modules that need it come from configs/build/structure.toml [kover].
        val structure = moduleStructure()
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
                if (structure.requiresCoverageThreshold(path)) {
                    verify {
                        rule {
                            minBound(structure.koverMinLineCoverage)
                        }
                    }
                }
            }
        }
    }
}

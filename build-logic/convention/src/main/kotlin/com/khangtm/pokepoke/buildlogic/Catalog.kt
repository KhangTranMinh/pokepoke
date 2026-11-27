package com.khangtm.pokepoke.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.pokepokeCatalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("pokepoke")

internal fun VersionCatalog.lib(alias: String): Provider<*> = findLibrary(alias).get()

internal fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

internal fun Project.addDep(configuration: String, alias: String) {
    dependencies.add(configuration, pokepokeCatalog.findLibrary(alias).get())
}

package com.khangtm.pokepoke.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

/** SDK, JVM toolchain and Java compatibility shared by application and library modules. */
internal fun Project.configureAndroidCommon(android: CommonExtension) {
    val catalog = pokepokeCatalog
    val jvm = catalog.version("jvm").toInt()
    android.compileSdk {
        version = release(catalog.version("compileSdk").toInt()) {
            minorApiLevel = catalog.version("compileSdkMinor").toInt()
        }
    }
    android.defaultConfig.minSdk = catalog.version("minSdk").toInt()
    android.compileOptions.apply {
        sourceCompatibility = JavaVersion.toVersion(jvm)
        targetCompatibility = JavaVersion.toVersion(jvm)
    }
    configureJvmToolchain()
}

internal fun Project.configureJvmToolchain() {
    val jvm = pokepokeCatalog.version("jvm").toInt()
    extensions.configure<KotlinBaseExtension> { jvmToolchain(jvm) }
}

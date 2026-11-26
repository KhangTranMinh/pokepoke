package com.khangtm.pokepoke.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        val version = readAppVersion()
        // Debug key is committed, so reading it eagerly is safe. The release key is only read lazily (below).
        val debugSigning = signingConfigInfo("debug", requiredFor = "mọi variant trừ prodRelease").get()
        val releaseSigning = signingConfigInfo("release", requiredFor = "prodRelease")
        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)
            defaultConfig {
                targetSdk = libs.version("targetSdk").toInt()
                versionCode = version.code
                versionName = version.name
            }
            buildFeatures.buildConfig = true
            androidResources.generateLocaleConfig = true
            val sharedDebug = signingConfigs.create("sharedDebug") {
                storeFile = debugSigning.storeFile
                storePassword = debugSigning.storePassword
                keyAlias = debugSigning.keyAlias
                keyPassword = debugSigning.keyPassword
            }
            flavorDimensions += "env"
            productFlavors {
                create("dev") {
                    dimension = "env"
                    applicationIdSuffix = ".dev"
                    versionNameSuffix = "-dev"
                    buildConfigField("String", "BASE_URL", "\"https://pokeapi.co/api/v2/\"")
                }
                create("qa") {
                    dimension = "env"
                    applicationIdSuffix = ".qa"
                    versionNameSuffix = "-qa"
                    buildConfigField("String", "BASE_URL", "\"https://pokeapi.co/api/v2/\"")
                }
                create("prod") {
                    dimension = "env"
                    buildConfigField("String", "BASE_URL", "\"https://pokeapi.co/api/v2/\"")
                }
            }
            buildTypes {
                debug {
                    signingConfig = sharedDebug
                    applicationIdSuffix = ".debug"
                    versionNameSuffix = "-debug"
                }
                release {
                    optimization {
                        enable = true
                        packageScope.set(setOf("androidx.**", "kotlin.**", "kotlinx.**"))
                    }
                    // Default for qaRelease; prodRelease is switched to the release key in onVariants.
                    signingConfig = sharedDebug
                }
            }
        }
        extensions.configure<ApplicationAndroidComponentsExtension> {
            beforeVariants(selector().withFlavor("env" to "dev").withBuildType("release")) { it.enable = false }
            onVariants(selector().withFlavor("env" to "prod").withBuildType("release")) { variant ->
                // A provider: a missing / incomplete release.properties fails only when prodRelease signing runs.
                variant.signingConfig.from(releaseSigning)
            }
        }
        registerVerifyModuleDependencies()
        registerVerifyReleaseHasNoDebugTool()
    }
}

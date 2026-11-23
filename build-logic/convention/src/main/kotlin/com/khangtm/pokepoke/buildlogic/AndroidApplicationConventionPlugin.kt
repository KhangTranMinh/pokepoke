package com.khangtm.pokepoke.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        val version = readAppVersion()
        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)
            defaultConfig {
                targetSdk = libs.version("targetSdk").toInt()
                versionCode = version.code
                versionName = version.name
            }
            buildFeatures.buildConfig = true
            androidResources.generateLocaleConfig = true
            buildTypes {
                debug {
                    applicationIdSuffix = ".debug"
                    versionNameSuffix = "-debug"
                }
                release {
                    optimization {
                        enable = true
                        packageScope.set(setOf("androidx.**", "kotlin.**", "kotlinx.**"))
                    }
                    // Temporary: sign with the debug keystore until a real release keystore exists.
                    signingConfig = signingConfigs.getByName("debug")
                }
            }
        }
        registerVerifyModuleDependencies()
        registerVerifyReleaseHasNoDebugTool()
    }
}

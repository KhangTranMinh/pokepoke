package com.khangtm.pokepoke.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Gradle build qua 3 pha: 1. Initialization (settings) -> 2. Configuration (áp plugin, chạy build script,
 * AGP tạo variant, cấu hình task) -> 3. Execution (chạy các task trong task graph).
 * Signing release được thiết kế để chỉ đọc file ở pha 3 và chỉ khi cần.
 * Chi tiết: docs/design/build.md §4a.1.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        val version = readAppVersion()
        // Pha 2 (áp plugin): đọc NGAY debug.properties vì DSL signingConfigs cần giá trị lúc cấu hình.
        // An toàn vì debug.* được commit nên luôn có.
        val debugSigning = signingConfigInfo("debug", requiredFor = "mọi variant trừ prodRelease").get()
        // Release chỉ là Provider: KHÔNG đọc file ở đây (xem variant.signingConfig.from bên dưới).
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
            // Flavor dev/qa/prod (dimension "env") chỉ khai báo ở pha 2; AGP ghép với build type để tạo variant.
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
                    // Mặc định cho mọi variant (kể cả qaRelease) ký bằng khóa debug;
                    // prodRelease được đổi sang khóa release trong onVariants.
                    signingConfig = sharedDebug
                }
            }
        }
        // Variant API của AGP (`androidComponents`). Lambda này chạy NGAY lúc áp plugin, nhưng chỉ đăng ký
        // callback; các callback chạy sau, khi AGP tạo variant.
        extensions.configure<ApplicationAndroidComponentsExtension> {
            // Chạy sau khi build script của :app xong, trước khi AGP tạo variant. selector() khớp mọi variant;
            // withFlavor(dev).withBuildType(release) thu hẹp còn devRelease. `it` là VariantBuilder:
            // enable = false => không có variant devRelease và không có task nào của nó.
            beforeVariants(selector().withFlavor("env" to "dev").withBuildType("release")) { it.enable = false }
            // Chạy sau khi AGP đã tạo variant, trước khi tạo task. `variant` đã gộp giá trị DSL
            // (lúc này signingConfig = sharedDebug); các thuộc tính là Property / Provider.
            onVariants(selector().withFlavor("env" to "prod").withBuildType("release")) { variant ->
                // Chỉ NỐI provider vào prodRelease (thay sharedDebug), chưa đọc file. File release.properties chỉ
                // được đọc khi task prodRelease nằm trong task graph (configuration cache store hoặc lúc task chạy).
                // Không dùng DSL signingConfigs.release hay signing ở flavor prod vì: DSL cần giá trị lúc cấu hình
                // (thiếu file sẽ hỏng mọi variant), và signingConfig của build type thắng flavor.
                // Ví dụ: assembleDevDebug xanh khi không có file release; assembleProdRelease fail với message rõ ràng.
                variant.signingConfig.from(releaseSigning)
            }
        }
        registerVerifyModuleDependencies()
        registerVerifyReleaseHasNoDebugTool()
    }
}

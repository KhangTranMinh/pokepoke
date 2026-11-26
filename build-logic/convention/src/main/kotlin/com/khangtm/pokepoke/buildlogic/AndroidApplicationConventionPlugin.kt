package com.khangtm.pokepoke.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.TestComponent
import org.gradle.api.Plugin
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Gradle build qua 3 pha: 1. Initialization (settings) -> 2. Configuration (áp plugin, chạy build script,
 * AGP tạo variant, cấu hình task) -> 3. Execution (chạy các task trong task graph).
 * Signing release được thiết kế để chỉ đọc file ở pha 3 và chỉ khi cần.
 * Chi tiết: docs/design/build.md §4a.1.
 * Config môi trường (các file TOML trong configs/env/) cùng nguyên tắc: phần được commit đọc ở pha 2, secret đọc lười ở pha 3
 * (docs/design/build.md §4b, code: AppEnvConfig.kt).
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
        // Pha 2 (áp plugin): đọc NGAY config.toml + secrets.example.toml (được commit nên luôn có) và kiểm tra mọi luật
        // (docs/design/build.md §4b). Đọc qua providers.fileContents nên là input của configuration cache.
        // Lỗi ở đây làm hỏng mọi lệnh Gradle, đúng ý: hai file này sai thì không variant nào đúng.
        val envConfig = loadEnvConfigOrFail(
            configText = providers.fileContents(rootProject.layout.projectDirectory.file(CONFIG_FILE)).asText.get(),
            secretsExampleText = providers.fileContents(rootProject.layout.projectDirectory.file(SECRETS_EXAMPLE_FILE)).asText.get(),
        )
        // secrets.toml chỉ là Provider: KHÔNG đọc ở đây. orElse("") để thiếu file không ném ở pha 2.
        val secretsText = providers.fileContents(rootProject.layout.projectDirectory.file(SECRETS_FILE)).asText.orElse("")
        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)
            defaultConfig {
                targetSdk = libs.version("targetSdk").toInt()
                versionCode = version.code
                versionName = version.name
            }
            buildFeatures.buildConfig = true
            // Robolectric needs the merged manifest / resources for :app unit tests.
            testOptions.unitTests.isIncludeAndroidResources = true
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
                }
                create("qa") {
                    dimension = "env"
                    applicationIdSuffix = ".qa"
                    versionNameSuffix = "-qa"
                }
                create("prod") {
                    dimension = "env"
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
            // Chạy cho từng variant (trừ devRelease đã tắt), sau khi AGP tạo variant, trước khi tạo task.
            // Giá trị theo môi trường KHÔNG nằm trong build-logic: chỉ đọc từ configs/env/ (docs/design/build.md §4b).
            onVariants { variant ->
                val flavor = variant.productFlavors.firstOrNull { it.first == "env" }?.second
                    ?: throw GradleException("Variant ${variant.name} không có flavor dimension 'env'.")
                // config.toml: giá trị đã có (pha 2) -> đặt thẳng vào BuildConfig và manifest placeholder.
                // buildConfigFields là MapProperty; chỉ null nếu buildFeatures.buildConfig = false.
                for ((key, value) in envConfig.values.getValue(flavor)) {
                    variant.buildConfigFields?.put(
                        key,
                        BuildConfigField(value.javaType, value.javaLiteral, "from configs/env/config.toml"),
                    )
                }
                // Placeholder dùng trong manifest dạng ${KEY} (vd. ${DEEPLINK_SCHEME}). Chỉ key của config.toml:
                // secret KHÔNG bao giờ thành placeholder vì manifest đọc được bằng `aapt2 dump`.
                variant.manifestPlaceholders.putAll(envConfig.placeholders(flavor))
                // Component test lồng trong variant (unit test của Robolectric, androidTest) tự merge manifest riêng
                // và không thừa kế placeholder của variant; thiếu thì merge manifest của test fail.
                variant.nestedComponents.filterIsInstance<TestComponent>().forEach {
                    it.manifestPlaceholders.putAll(envConfig.placeholders(flavor))
                }
                // secrets.toml: chỉ NỐI provider vào từng field, chưa đọc file. File chỉ được đọc khi task
                // generate<Variant>BuildConfig của variant này nằm trong task graph (lúc lưu configuration cache
                // hoặc lúc task chạy); thiếu / sai chỉ làm hỏng variant cần secret. Không có key secret nào
                // trong secrets.example.toml -> không có provider, không cần secrets.toml.
                if (envConfig.secretKeys.isNotEmpty()) {
                    val flavorSecrets = secretsText.map { text ->
                        try {
                            resolveSecrets(flavor, envConfig.secretKeys, text)
                        } catch (e: EnvConfigException) {
                            throw GradleException(e.message.orEmpty(), e)
                        }
                    }
                    for (key in envConfig.secretKeys) {
                        variant.buildConfigFields?.put(
                            key,
                            flavorSecrets.map { BuildConfigField("String", javaStringLiteral(it.getValue(key)), "secret") },
                        )
                    }
                }
            }
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

/** Như [loadEnvConfig] nhưng đổi lỗi nghiệp vụ thành `GradleException` (message hiện thẳng ở output của Gradle). */
private fun loadEnvConfigOrFail(configText: String, secretsExampleText: String): EnvConfig =
    try {
        loadEnvConfig(configText, secretsExampleText)
    } catch (e: EnvConfigException) {
        throw GradleException(e.message.orEmpty(), e)
    }

package com.khangtm.pokepoke.buildlogic

import com.android.build.api.variant.SigningConfigInfo
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.io.File
import java.util.Properties

/**
 * Lazy reader of `configs/signing/<name>.properties` (docs/design/build.md §4a).
 *
 * The file is read through `providers.fileContents` (configuration-cache input). Nothing is read or
 * validated until the returned provider is queried, so a missing file only fails whoever asks for it.
 * `storeFile` is relative to `configs/signing/`.
 */
internal fun Project.signingConfigInfo(name: String, requiredFor: String): Provider<SigningConfigInfo> {
    val dir = rootProject.layout.projectDirectory.dir("configs/signing")
    val propertiesFile = dir.file("$name.properties")
    val path = "configs/signing/$name.properties"
    return providers.fileContents(propertiesFile).asText.orElse("").map { text ->
        if (text.isBlank()) {
            throw GradleException("Thiếu $path — chỉ cần cho $requiredFor (xem docs/design/build.md §4a).")
        }
        val props = Properties().apply { load(text.reader()) }
        fun value(key: String): String = props.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw GradleException("$path: thiếu key '$key'.")
        val storeFile = File(value("storeFile")).let { if (it.isAbsolute) it else File(dir.asFile, it.path) }
        if (!storeFile.isFile) {
            throw GradleException("$path: storeFile '${value("storeFile")}' không tồn tại trong configs/signing/.")
        }
        SigningConfigInfo(
            storeFile = storeFile,
            storePassword = value("storePassword"),
            keyAlias = value("keyAlias"),
            keyPassword = value("keyPassword"),
            storeType = "jks",
        )
    }
}

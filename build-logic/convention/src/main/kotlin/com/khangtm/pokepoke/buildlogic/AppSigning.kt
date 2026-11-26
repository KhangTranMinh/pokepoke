package com.khangtm.pokepoke.buildlogic

import com.android.build.api.variant.SigningConfigInfo
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.io.File
import java.util.Properties

/**
 * Lazy reader of `configs/signing/<name>.properties` (docs/design/build.md §4a, §4a.1).
 *
 * - Trả về một `Provider` lười: gọi hàm này **không** đọc file nào. Chỉ khi có ai query provider
 *   (`.get()`, hoặc Gradle query khi cần giá trị) thì file mới được đọc và kiểm tra.
 * - Với release, provider chỉ bị query khi task cần nó nằm trong task graph (ví dụ `packageProdRelease`),
 *   lúc lưu configuration cache hoặc lúc task chạy. Nhờ vậy `assembleDevDebug` không cần file release.
 * - File đọc qua `providers.fileContents(...)` nên là input của configuration cache: sửa file thì cache làm mới.
 * - Lỗi ném ra khi query: thiếu file (hoặc file rỗng), thiếu key trong `.properties`, hoặc `storeFile` không tồn tại.
 * - `storeFile` là đường dẫn tương đối với `configs/signing/` (đường dẫn tuyệt đối vẫn được chấp nhận).
 * - `storeType = "jks"` vẫn đọc được keystore PKCS12 do `keytool` mới tạo.
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

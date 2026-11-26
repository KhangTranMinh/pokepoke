package com.khangtm.pokepoke.buildlogic

import org.tomlj.Toml
import org.tomlj.TomlTable

/**
 * Config theo môi trường từ các file TOML trong `configs/env/` (docs/design/build.md §4b).
 *
 * File này chỉ gồm **hàm thuần** (chuỗi vào, giá trị ra, không đụng Gradle / file) nên test được bằng unit test
 * thường. Phần gắn vào Gradle (đọc file lúc nào, đặt vào variant ra sao) nằm ở `AndroidApplicationConventionPlugin`.
 *
 * Luồng: `parseEnvSections` (một file -> section) -> `mergeFlavors` (common + ghi đè) -> `requireSameKeys`
 * (ba flavor cùng bộ key) -> `loadEnvConfig` (config + secrets.example, chồng chéo key) -> `resolveSecrets`
 * (secrets.toml thật, chỉ gọi lười khi variant cần).
 */

internal const val ENV_COMMON = "common"
internal val ENV_FLAVORS = listOf("dev", "qa", "prod")
private val ENV_SECTIONS = listOf(ENV_COMMON) + ENV_FLAVORS
private val KEY_REGEX = Regex("[A-Z][A-Z0-9]*(_[A-Z0-9]+)*")

internal const val CONFIG_FILE = "configs/env/config.toml"
internal const val SECRETS_EXAMPLE_FILE = "configs/env/secrets.example.toml"
internal const val SECRETS_FILE = "configs/env/secrets.toml"

/** Một giá trị config; kiểu lấy từ TOML. `javaType` / `javaLiteral` dùng cho field `BuildConfig`. */
internal sealed interface EnvValue {
    val javaType: String
    val javaLiteral: String

    /** Giá trị dạng chuỗi dùng cho manifest placeholder. */
    val text: String

    data class Str(val value: String) : EnvValue {
        override val javaType get() = "String"
        override val javaLiteral get() = javaStringLiteral(value)
        override val text get() = value
    }

    data class Num(val value: Long) : EnvValue {
        override val javaType get() = "long"
        override val javaLiteral get() = "${value}L"
        override val text get() = value.toString()
    }

    data class Flag(val value: Boolean) : EnvValue {
        override val javaType get() = "boolean"
        override val javaLiteral get() = value.toString()
        override val text get() = value.toString()
    }
}

/** Kết quả đọc `config.toml` + `secrets.example.toml`: đã gộp, đã kiểm tra mọi luật không cần `secrets.toml`. */
internal data class EnvConfig(
    /** flavor -> (key -> giá trị) của `config.toml` sau khi gộp `[common]` + ghi đè. */
    val values: Map<String, Map<String, EnvValue>>,
    /** Bộ key secret bắt buộc (key của `secrets.example.toml`; ba flavor giống nhau). */
    val secretKeys: Set<String>,
)

internal fun javaStringLiteral(value: String): String = buildString {
    append('"')
    for (c in value) {
        when {
            c == '\\' -> append("\\\\")
            c == '"' -> append("\\\"")
            c == '\n' -> append("\\n")
            c == '\r' -> append("\\r")
            c == '\t' -> append("\\t")
            c.code < 0x20 || c.code > 0x7e -> append("\\u%04x".format(c.code))
            else -> append(c)
        }
    }
    append('"')
}

/**
 * Parse một file TOML thành section -> (key -> giá trị). Kiểm tra: cú pháp, đủ đúng 4 section
 * (`common`, `dev`, `qa`, `prod`), không bảng con / mảng / kiểu khác chuỗi-số nguyên-bool, tên key hợp lệ.
 * `stringsOnly` (secrets): mọi giá trị phải là chuỗi.
 */
internal fun parseEnvSections(file: String, text: String, stringsOnly: Boolean = false): Map<String, Map<String, EnvValue>> {
    val toml = Toml.parse(text)
    if (toml.hasErrors()) {
        val first = toml.errors().first()
        envFail("$file: lỗi cú pháp TOML: ${first.message} (${first.position()}).")
    }
    for (key in toml.keySet()) {
        if (key !in ENV_SECTIONS) {
            envFail("$file: '$key' không hợp lệ ở cấp ngoài cùng; chỉ có section ${ENV_SECTIONS.joinToString { "[$it]" }}.")
        }
    }
    return ENV_SECTIONS.associateWith { section ->
        val table = toml.get(section) as? TomlTable
            ?: envFail("$file: thiếu section [$section] (cần đủ ${ENV_SECTIONS.joinToString { "[$it]" }}, được để rỗng).")
        table.keySet().associateWith { key -> parseValue(file, section, key, table.get(key), stringsOnly) }
    }
}

private fun parseValue(file: String, section: String, key: String, raw: Any?, stringsOnly: Boolean): EnvValue {
    if (!KEY_REGEX.matches(key)) {
        envFail("$file [$section]: tên key '$key' phải là UPPER_SNAKE_CASE (vì thành tên field BuildConfig).")
    }
    val value = when (raw) {
        is String -> EnvValue.Str(raw)
        is Long -> EnvValue.Num(raw)
        is Boolean -> EnvValue.Flag(raw)
        is TomlTable -> envFail("$file [$section]: '$key' là bảng con; không hỗ trợ (chỉ chuỗi, số nguyên, true / false).")
        else -> envFail(
            "$file [$section]: '$key' có kiểu ${raw?.javaClass?.simpleName} không hỗ trợ (chỉ chuỗi, số nguyên, true / false).",
        )
    }
    if (stringsOnly && value !is EnvValue.Str) {
        envFail("$file [$section]: secret '$key' phải là chuỗi.")
    }
    return value
}

/** Gộp `[common]` + phần ghi đè của từng flavor; ghi đè phải cùng kiểu với `[common]`. */
internal fun mergeFlavors(file: String, sections: Map<String, Map<String, EnvValue>>): Map<String, Map<String, EnvValue>> {
    val common = sections.getValue(ENV_COMMON)
    return ENV_FLAVORS.associateWith { flavor ->
        val overrides = sections.getValue(flavor)
        for ((key, value) in overrides) {
            val base = common[key]
            if (base != null && base.javaType != value.javaType) {
                envFail(
                    "$file [$flavor]: '$key' ghi đè khác kiểu với [common] " +
                        "(${base.javaType} -> ${value.javaType}).",
                )
            }
        }
        common + overrides
    }
}

/** Sau khi gộp, ba flavor phải có cùng bộ key (tránh dev có key mà prod thiếu -> crash lúc chạy). */
internal fun requireSameKeys(file: String, merged: Map<String, Map<String, EnvValue>>) {
    val all = merged.values.flatMapTo(sortedSetOf()) { it.keys }
    for (flavor in ENV_FLAVORS) {
        val missing = all - merged.getValue(flavor).keys
        if (missing.isNotEmpty()) {
            envFail("$file: flavor '$flavor' thiếu key ${missing.joinToString { "'$it'" }}; ba flavor phải có cùng bộ key.")
        }
    }
}

/**
 * Đọc phần luôn có mặt (được commit): `config.toml` + `secrets.example.toml`. Chạy ở pha cấu hình;
 * lỗi ở đây làm hỏng mọi lệnh Gradle (file được commit nên luôn phải đúng).
 */
internal fun loadEnvConfig(configText: String, secretsExampleText: String): EnvConfig {
    val values = mergeFlavors(CONFIG_FILE, parseEnvSections(CONFIG_FILE, configText))
    requireSameKeys(CONFIG_FILE, values)

    val example = parseEnvSections(SECRETS_EXAMPLE_FILE, secretsExampleText, stringsOnly = true)
    for ((section, entries) in example) {
        for ((key, value) in entries) {
            if ((value as EnvValue.Str).value.isNotEmpty()) {
                envFail("$SECRETS_EXAMPLE_FILE [$section]: '$key' phải là \"\" (file mẫu được commit, không chứa giá trị thật).")
            }
        }
    }
    val secrets = mergeFlavors(SECRETS_EXAMPLE_FILE, example)
    requireSameKeys(SECRETS_EXAMPLE_FILE, secrets)

    for (flavor in ENV_FLAVORS) {
        val both = values.getValue(flavor).keys intersect secrets.getValue(flavor).keys
        if (both.isNotEmpty()) {
            envFail("Key ${both.joinToString { "'$it'" }} nằm ở cả $CONFIG_FILE lẫn $SECRETS_EXAMPLE_FILE (flavor '$flavor'); chỉ được ở một nơi.")
        }
    }
    return EnvConfig(values, secrets.getValue(ENV_FLAVORS.first()).keys)
}

/**
 * Giá trị secret thật của [flavor] từ nội dung `secrets.toml` ([secretsText], rỗng nếu file không có).
 * Chỉ gọi khi [required] không rỗng (variant cần secret). Thiếu key, giá trị rỗng, hoặc key thừa -> fail.
 */
internal fun resolveSecrets(flavor: String, required: Set<String>, secretsText: String): Map<String, String> {
    if (secretsText.isBlank()) {
        envFail(
            "Thiếu $SECRETS_FILE — cần cho secret ${required.sorted().joinToString { "'$it'" }} của flavor '$flavor' " +
                "(sao chép $SECRETS_EXAMPLE_FILE rồi điền giá trị; xem docs/design/build.md §4b).",
        )
    }
    val merged = mergeFlavors(SECRETS_FILE, parseEnvSections(SECRETS_FILE, secretsText, stringsOnly = true))
    val actual = merged.getValue(flavor)
    for (key in required.sorted()) {
        val value = actual[key] ?: envFail("Thiếu secret '$key' cho flavor '$flavor' trong $SECRETS_FILE.")
        if ((value as EnvValue.Str).value.isEmpty()) {
            envFail("Secret '$key' của flavor '$flavor' trong $SECRETS_FILE đang rỗng.")
        }
    }
    val extra = actual.keys - required
    if (extra.isNotEmpty()) {
        envFail("$SECRETS_FILE (flavor '$flavor') có key thừa ${extra.sorted().joinToString { "'$it'" }}; không có trong $SECRETS_EXAMPLE_FILE.")
    }
    return required.associateWith { (actual.getValue(it) as EnvValue.Str).value }
}

/** Manifest placeholder của một flavor: mọi key của `config.toml` (secret không bao giờ có mặt ở đây). */
internal fun EnvConfig.placeholders(flavor: String): Map<String, String> =
    values.getValue(flavor).mapValues { it.value.text }

internal class EnvConfigException(message: String) : RuntimeException(message)

private fun envFail(message: String): Nothing = throw EnvConfigException(message)

package com.khangtm.pokepoke.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tomlj.Toml
import org.tomlj.TomlParseResult
import org.tomlj.TomlTable
import java.io.File

/**
 * Ba version catalog của repo: `pokepoke` (build-logic đọc / tự thêm), `coreLibs` (thư viện ngoài của `:core:*`)
 * và `libs` (thư viện riêng của app). Mỗi artifact và mỗi plugin id chỉ nằm ở một catalog; `version.ref` chỉ trỏ [versions] cùng file; mọi alias plugin
 * gọi phải có trong `pokepoke.versions.toml`.
 */
class CatalogConsistencyTest {

    private val root = File("../..").canonicalFile // Gradle chạy test với working dir = build-logic/convention
    private val pokepokeText = File(root, "gradle/pokepoke.versions.toml").readText()
    private val libsText = File(root, "gradle/libs.versions.toml").readText()
    private val coreText = File(root, "gradle/core.versions.toml").readText()
    private val catalogs get() = mapOf("pokepoke" to pokepokeText, "coreLibs" to coreText, "libs" to libsText)

    // --- Logic kiểm tra (chạy được trên text giả để test chính nó) ---

    private fun parse(text: String): TomlParseResult =
        Toml.parse(text).also { check(!it.hasErrors()) { it.errors().joinToString() } }

    private fun libraries(text: String): Map<String, String> { // alias -> group:name
        val table: TomlTable = parse(text).getTable("libraries") ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        for (alias in table.keySet()) {
            val lib: TomlTable = checkNotNull(table.getTable(listOf(alias))) { "libraries.$alias phải là inline table" }
            result[alias] = "${lib.getString("group")}:${lib.getString("name")}"
        }
        return result
    }

    private fun pluginIds(text: String): Map<String, String> { // alias -> id
        val table: TomlTable = parse(text).getTable("plugins") ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        for (alias in table.keySet()) {
            result[alias] = checkNotNull(table.getTable(listOf(alias))?.getString("id")) { "plugins.$alias thiếu id" }
        }
        return result
    }

    private fun duplicates(a: Map<String, String>, b: Map<String, String>): List<String> =
        a.values.toSet().intersect(b.values.toSet()).sorted()

    /** `version.ref` không tồn tại trong [versions] cùng file. */
    private fun danglingVersionRefs(text: String): List<String> {
        val toml = parse(text)
        val versions = toml.getTable("versions")?.keySet().orEmpty()
        return listOf("libraries", "plugins").flatMap { section ->
            val table = toml.getTable(section) ?: return@flatMap emptyList()
            table.keySet().mapNotNull { alias ->
                // `version.ref = "x"` là inline table `version`; `version = "1.0"` là chuỗi (không có ref).
                val version = table.getTable(listOf(alias))?.get(listOf("version"))
                val ref = (version as? TomlTable)?.getString("ref")
                if (ref != null && ref !in versions) "$section.$alias -> version.ref \"$ref\"" else null
            }
        }
    }

    // --- Test trên file thật ---

    private fun <T> pairs(items: Map<String, T>): List<Triple<String, String, Pair<T, T>>> {
        val names = items.keys.toList()
        return names.indices.flatMap { i -> (i + 1 until names.size).map { j -> Triple(names[i], names[j], items.getValue(names[i]) to items.getValue(names[j])) } }
    }

    @Test
    fun `no artifact appears in two catalogs`() {
        for ((a, b, texts) in pairs(catalogs)) {
            val duplicated = duplicates(libraries(texts.first), libraries(texts.second))
            assertTrue("group:name nằm ở cả $a và $b: $duplicated", duplicated.isEmpty())
        }
    }

    @Test
    fun `no plugin id appears in two catalogs`() {
        for ((a, b, texts) in pairs(catalogs)) {
            val duplicated = duplicates(pluginIds(texts.first), pluginIds(texts.second))
            assertTrue("plugin id nằm ở cả $a và $b: $duplicated", duplicated.isEmpty())
        }
    }

    @Test
    fun `adding an artifact of coreLibs to libs is detected`() {
        val injected = libsText.replace(
            "[libraries]",
            "[libraries]\nretrofit = { group = \"com.squareup.retrofit2\", name = \"retrofit\", version = \"3.0.0\" }",
        )
        check(injected != libsText) { "không chèn được vào libs.versions.toml" }
        assertEquals(listOf("com.squareup.retrofit2:retrofit"), duplicates(libraries(coreText), libraries(injected)))
    }

    @Test
    fun `no duplicate artifact inside one catalog`() {
        for ((name, text) in catalogs) {
            val coordinates = libraries(text).values
            assertEquals("$name: trùng group:name", emptyList<String>(), coordinates.groupBy { it }.filter { it.value.size > 1 }.keys.toList())
        }
    }

    @Test
    fun `version refs resolve within the same file`() {
        for ((name, text) in catalogs) assertEquals(name, emptyList<String>(), danglingVersionRefs(text))
    }

    @Test
    fun `every library alias used by the plugins exists in pokepoke versions toml`() {
        val libraryAliases = libraries(pokepokeText).keys
        val missing = usedLibraryAliases().filter { it.replace('.', '-') !in libraryAliases }
        assertEquals("alias plugin gọi nhưng thiếu trong pokepoke.versions.toml", emptyList<String>(), missing)
    }

    @Test
    fun `every version alias read by the plugins exists in pokepoke versions toml`() {
        val versions = parse(pokepokeText).getTable("versions")?.keySet().orEmpty()
        val missing = usedVersionAliases().filter { it !in versions }
        assertEquals("version plugin đọc nhưng thiếu trong pokepoke.versions.toml", emptyList<String>(), missing)
    }

    @Test
    fun `every plugin id applied by the plugins exists in pokepoke versions toml`() {
        val ids = pluginIds(pokepokeText).values
        val missing = usedPluginIds().filter { it !in ids && it !in builtInPlugins }
        assertEquals("plugin id áp dụng nhưng thiếu trong [plugins] của pokepoke.versions.toml", emptyList<String>(), missing)
    }

    // --- Test chính bộ phát hiện (text giả) ---

    @Test
    fun `detector reports duplicates and dangling refs`() {
        val a = """
            [versions]
            junit = "4"
            [libraries]
            junit = { group = "junit", name = "junit", version.ref = "junit" }
            bad = { group = "x", name = "y", version.ref = "nope" }
        """.trimIndent()
        val b = """
            [versions]
            other = "1"
            [libraries]
            junit-again = { group = "junit", name = "junit", version.ref = "other" }
        """.trimIndent()
        assertEquals(listOf("junit:junit"), duplicates(libraries(a), libraries(b)))
        assertEquals(listOf("libraries.bad -> version.ref \"nope\""), danglingVersionRefs(a))
        assertEquals(emptyList<String>(), danglingVersionRefs(b))
    }

    // --- Quét code của plugin ---

    private val mainDir = File("src/main/kotlin/com/khangtm/pokepoke/buildlogic")
    private val builtInPlugins = setOf("java-test-fixtures")

    private fun pluginSources(): List<String> = checkNotNull(mainDir.listFiles()).filter { it.extension == "kt" }.map { it.readText() }

    /** `addDep("conf", "alias")`, `findLibrary("alias")` và các literal kebab-case trong danh sách alias của plugin. */
    private fun usedLibraryAliases(): Set<String> {
        val sources = pluginSources()
        val fromCalls = sources.flatMap { src ->
            Regex("""addDep\(\s*"[^"]+"\s*,\s*"([^"]+)"""").findAll(src).map { it.groupValues[1] }.toList() +
                Regex("""findLibrary\("([^"]+)"\)""").findAll(src).map { it.groupValues[1] }.toList()
        }
        val fromLists = sources.flatMap { src ->
            Regex("""listOf\(([^)]*)\)""").findAll(src).flatMap { list ->
                Regex(""""([a-z][a-z0-9]*(?:-[a-z0-9]+)+)"""").findAll(list.groupValues[1]).map { it.groupValues[1] }
            }.toList()
        }
        return (fromCalls + fromLists).toSet().also { assertTrue("không quét được alias nào", it.isNotEmpty()) }
    }

    private fun usedVersionAliases(): Set<String> =
        pluginSources().flatMap { src ->
            Regex("""(?:pokepokeCatalog|catalog)\.version\("([^"]+)"\)""").findAll(src).map { it.groupValues[1] }.toList() +
                Regex("""findVersion\("([^"]+)"\)""").findAll(src).map { it.groupValues[1] }.toList()
        }.toSet().also { assertTrue("không quét được version nào", it.isNotEmpty()) }

    private fun usedPluginIds(): Set<String> =
        pluginSources().flatMap { src ->
            Regex("""pluginManager\.apply\("([^"]+)"\)""").findAll(src).map { it.groupValues[1] }.toList()
        }.toSet().also { assertTrue("không quét được plugin id nào", it.isNotEmpty()) }
}

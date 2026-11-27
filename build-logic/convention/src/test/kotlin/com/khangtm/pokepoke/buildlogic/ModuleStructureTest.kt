package com.khangtm.pokepoke.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleStructureTest {

    private val projects = setOf(":app", ":core", ":core:testing", ":core:ui", ":translation", ":debugtool")

    private val valid = """
        [library]
        testImplementation = [":core:testing"]
        excludeAutoDependencies = [":core:testing", ":translation"]
        [feature.api]
        api = [":core:ui"]
        [feature.impl]
        implementation = [":core:ui"]
        testFixtures = [":core:ui"]
        [app]
        debugTool = ":debugtool"
        [kover]
        minLineCoverage = 70
        modules = [":core:*", ":debugtool"]
        [[rules]]
        from = ["**"]
        except = [":app"]
        to = [":debugtool"]
        reason = "only app"
    """.trimIndent()

    private fun parse(text: String) = parseModuleStructure(text, projects)

    private fun failure(text: String): String =
        try {
            parse(text)
            error("expected a ModuleStructureException")
        } catch (e: ModuleStructureException) {
            e.message.orEmpty()
        }

    private fun broken(old: String, new: String) = failure(valid.replace(old, new))

    @Test
    fun `star matches exactly one segment`() {
        assertTrue(matches(":core:*:impl", ":core:storage:impl"))
        assertFalse(matches(":core:*:impl", ":core:a:b:impl"))
        assertFalse(matches(":core:*:impl", ":core:impl"))
        assertTrue(matches(":core:*", ":core:ui"))
        assertFalse(matches(":core:*", ":core"))
    }

    @Test
    fun `double star matches one or more segments`() {
        assertTrue(matches(":core:**", ":core:ui"))
        assertTrue(matches(":core:**", ":core:storage:impl"))
        assertFalse(matches(":core:**", ":core"))
        assertTrue(matches(":**:impl", ":feature:home:impl"))
        assertTrue(matches(":**:impl", ":x:impl"))
        assertFalse(matches(":**:impl", ":impl"))
        assertFalse(matches(":**:impl", ":feature:home:api"))
    }

    @Test
    fun `double star alone matches every module`() {
        assertTrue(matches("**", ":app"))
        assertTrue(matches("**", ":feature:home:impl"))
    }

    @Test
    fun `exact path matches only itself`() {
        assertTrue(matches(":debugtool", ":debugtool"))
        assertFalse(matches(":debugtool", ":debugtool:x"))
        assertFalse(matches(":core:ui", ":core"))
        assertFalse(matches(":core:ui", ":core:ui:impl"))
    }

    @Test
    fun `valid file parses`() {
        val structure = parse(valid)
        assertEquals(listOf(":core:testing"), structure.libraryTestImplementation)
        assertEquals(70, structure.koverMinLineCoverage)
        assertEquals(":debugtool", structure.appDebugTool)
        assertEquals(listOf(ArchitectureRule(listOf("**"), listOf(":app"), listOf(":debugtool"), "only app")), structure.rules)
        assertFalse(structure.receivesLibraryAutoDependencies(":translation"))
        assertTrue(structure.receivesLibraryAutoDependencies(":core:ui"))
        assertTrue(structure.requiresCoverageThreshold(":core:ui"))
        assertFalse(structure.requiresCoverageThreshold(":app"))
    }

    @Test
    fun `the real structure toml of the repo parses`() {
        val structure = RealStructure.structure
        assertEquals(listOf(":core:testing"), structure.libraryTestImplementation)
        assertEquals(":debugtool", structure.appDebugTool)
        assertEquals(70, structure.koverMinLineCoverage)
        assertEquals(5, structure.rules.size)
    }

    @Test
    fun `coverage threshold applies to the expected modules only`() {
        val structure = RealStructure.structure
        val required = listOf(":core:common-kotlin", ":core:common-android", ":core:network", ":core:connectivity", ":core:storage:impl", ":business:domain", ":business:data", ":feature:home:impl", ":debugtool")
        val notRequired = listOf(":core:designsystem", ":core:storage:api", ":feature:home:api", ":core:testing", ":app")
        required.forEach { assertTrue(it, structure.requiresCoverageThreshold(it)) }
        notRequired.forEach { assertFalse(it, structure.requiresCoverageThreshold(it)) }
    }

    @Test
    fun `unknown top level section fails`() {
        assertTrue(failure("$valid\n[extra]\nx = 1\n").contains("unknown top-level key 'extra'"))
    }

    @Test
    fun `unknown key in a section fails`() {
        assertTrue(broken("[app]\ndebugTool", "[app]\nfoo = 1\ndebugTool").contains("[app]: unknown key 'foo'"))
    }

    @Test
    fun `missing key fails`() {
        assertTrue(broken("modules = [\":core:*\", \":debugtool\"]", "").contains("[kover]: missing key 'modules'"))
    }

    @Test
    fun `wrong type fails`() {
        assertTrue(broken("api = [\":core:ui\"]", "api = \":core:ui\"").contains("'api' must be an array of strings"))
        assertTrue(broken("minLineCoverage = 70", "minLineCoverage = \"70\"").contains("'minLineCoverage' must be an integer"))
        assertTrue(broken("debugTool = \":debugtool\"", "debugTool = 1").contains("'debugTool' must be a string"))
        assertTrue(broken("api = [\":core:ui\"]", "api = [1]").contains("'api[0]' must be a string"))
    }

    @Test
    fun `min line coverage outside 0 to 100 fails`() {
        assertTrue(broken("minLineCoverage = 70", "minLineCoverage = 101").contains("out of range 0..100"))
        assertTrue(broken("minLineCoverage = 70", "minLineCoverage = -1").contains("out of range 0..100"))
        assertEquals(100, parse(valid.replace("minLineCoverage = 70", "minLineCoverage = 100")).koverMinLineCoverage)
    }

    @Test
    fun `rule missing from to or reason fails`() {
        assertTrue(broken("from = [\"**\"]\n", "").contains("missing key 'from'"))
        assertTrue(broken("to = [\":debugtool\"]\nreason", "reason").contains("missing key 'to'"))
        assertTrue(broken("reason = \"only app\"", "").contains("missing key 'reason'"))
        assertTrue(broken("reason = \"only app\"", "reason = \" \"").contains("'reason' must not be blank"))
        assertTrue(broken("reason = \"only app\"", "reason = \"x\"\nwhy = \"y\"").contains("unknown key 'why'"))
    }

    @Test
    fun `exact path missing from settings fails`() {
        val message = broken("debugTool = \":debugtool\"", "debugTool = \":debugtol\"")
        assertTrue(message, message.contains("[app] debugTool = ':debugtol'") && message.contains("no such project"))
        assertTrue(broken("api = [\":core:ui\"]", "api = [\":core:uii\"]").contains("no such project"))
        assertTrue(broken("except = [\":app\"]", "except = [\":ap\"]").contains("no such project"))
    }

    @Test
    fun `pattern matching no project fails`() {
        val message = broken("modules = [\":core:*\", \":debugtool\"]", "modules = [\":feature:*:impl\"]")
        assertTrue(message, message.contains("matches no project"))
        assertTrue(broken("to = [\":debugtool\"]", "to = [\":nothing:**\"]").contains("matches no project"))
    }

    @Test
    fun `malformed paths and patterns fail`() {
        assertTrue(broken("debugTool = \":debugtool\"", "debugTool = \"debugtool\"").contains("starts with ':'"))
        assertTrue(broken("modules = [\":core:*\", \":debugtool\"]", "modules = [\":core:u*\"]").contains("must be exactly '*' or '**'"))
        assertTrue(broken("modules = [\":core:*\", \":debugtool\"]", "modules = [\":core::ui\"]").contains("empty path segment"))
        assertTrue(broken("debugTool = \":debugtool\"", "debugTool = \":core:*\"").contains("wildcards are not allowed"))
    }

    @Test
    fun `toml syntax error fails`() {
        assertTrue(failure("[library\n").contains("TOML syntax error"))
    }
}

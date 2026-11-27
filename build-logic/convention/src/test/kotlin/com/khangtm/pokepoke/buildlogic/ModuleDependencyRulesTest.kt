package com.khangtm.pokepoke.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleDependencyRulesTest {

    private fun violations(module: String, vararg deps: String) =
        ModuleDependencyRules.violations(RealStructure.structure.rules, module, deps.toList())

    @Test
    fun `app may depend on impl and debugtool`() {
        assertTrue(violations(":app", ":feature:home:impl", ":debugtool", ":core:common-kotlin").isEmpty())
    }

    @Test
    fun `non app module must not depend on another impl`() {
        val result = violations(":feature:details:impl", ":feature:home:impl")
        assertEquals(1, result.size)
        assertTrue(result.single().contains(":feature:details:impl"))
        assertTrue(result.single().contains(":feature:home:impl"))
    }

    @Test
    fun `non app module must not depend on debugtool`() {
        assertEquals(1, violations(":feature:home:impl", ":debugtool").size)
    }

    @Test
    fun `core must not depend on business or feature`() {
        assertEquals(3, violations(":core:ui", ":business:domain", ":business:data", ":feature:home:api").size)
        assertTrue(violations(":core:ui", ":core:designsystem", ":core:common-kotlin").isEmpty())
    }

    @Test
    fun `business must not depend on feature but may depend on core`() {
        assertEquals(1, violations(":business:data", ":feature:home:api").size)
        assertEquals(1, violations(":business:domain", ":feature:home:api").size)
        assertTrue(violations(":business:data", ":business:domain", ":core:network", ":core:connectivity").isEmpty())
        assertTrue(violations(":business:domain", ":core:common-kotlin").isEmpty())
    }

    @Test
    fun `only app may depend on business data`() {
        assertTrue(violations(":app", ":business:data", ":business:domain").isEmpty())
        val result = violations(":feature:home:impl", ":business:data")
        assertEquals(1, result.size)
        assertTrue(result.single().contains(":business:data"))
        assertEquals(1, violations(":business:domain", ":business:data").size)
        assertEquals(1, violations(":debugtool", ":business:data").size)
        assertTrue(violations(":feature:home:impl", ":business:domain").isEmpty())
    }

    @Test
    fun `debugtool must not depend on business or feature`() {
        assertEquals(3, violations(":debugtool", ":business:domain", ":business:data", ":feature:home:api").size)
        assertTrue(violations(":debugtool", ":core:common-kotlin").isEmpty())
    }

    @Test
    fun `core testing must not depend on impl`() {
        assertEquals(1, violations(":core:testing", ":core:storage:impl").size)
    }

    @Test
    fun `translation must not depend on any module`() {
        assertEquals(2, violations(":translation", ":core:common-kotlin", ":feature:home:api").size)
        assertTrue(violations(":translation").isEmpty())
    }

    @Test
    fun `every module may depend on translation`() {
        listOf(":app", ":core:ui", ":core:designsystem", ":business:data", ":feature:home:impl", ":debugtool").forEach {
            assertTrue(it, violations(it, ":translation").isEmpty())
        }
    }

    @Test
    fun `impl may depend on its own api`() {
        assertTrue(violations(":feature:home:impl", ":feature:home:api", ":business:domain").isEmpty())
    }

    @Test
    fun `message contains the reason from structure toml`() {
        val message = violations(":feature:favourite:impl", ":feature:home:impl").single()
        assertTrue(message, message.contains("only :app may depend on :impl / :debugtool / :business:data"))
        assertTrue(violations(":core:ui", ":feature:home:api").single().contains(":core must not depend on :business or :feature"))
    }

    @Test
    fun `except removes a module from a rule`() {
        val rules = listOf(ArchitectureRule(listOf("**"), listOf(":app", ":core:**"), listOf(":x"), "r"))
        assertTrue(ModuleDependencyRules.violations(rules, ":app", listOf(":x")).isEmpty())
        assertTrue(ModuleDependencyRules.violations(rules, ":core:ui", listOf(":x")).isEmpty())
        assertEquals(1, ModuleDependencyRules.violations(rules, ":feature:a", listOf(":x")).size)
    }

    @Test
    fun `a module depending on itself is ignored and duplicates are reported once`() {
        assertTrue(violations(":feature:home:impl", ":feature:home:impl").isEmpty())
        assertEquals(1, violations(":core:ui", ":feature:home:api", ":feature:home:api").size)
    }
}

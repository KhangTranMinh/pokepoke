package com.khangtm.pokepoke.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleDependencyRulesTest {

    private fun violations(module: String, vararg deps: String) =
        ModuleDependencyRules.violations(module, deps.toList())

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
    fun `core must not depend on data or feature`() {
        assertEquals(2, violations(":core:ui", ":data:pokemon:api", ":feature:home:api").size)
        assertTrue(violations(":core:ui", ":core:designsystem", ":core:common-kotlin").isEmpty())
    }

    @Test
    fun `data must not depend on feature but may depend on core`() {
        assertEquals(1, violations(":data:pokemon:impl", ":feature:home:api").size)
        assertTrue(violations(":data:pokemon:impl", ":data:pokemon:api", ":core:storage:api").isEmpty())
    }

    @Test
    fun `debugtool must not depend on data or feature`() {
        assertEquals(2, violations(":debugtool", ":data:pokemon:api", ":feature:home:api").size)
        assertTrue(violations(":debugtool", ":core:common-kotlin").isEmpty())
    }

    @Test
    fun `core testing must not depend on impl`() {
        assertEquals(1, violations(":core:testing", ":core:network:impl").size)
    }

    @Test
    fun `impl may depend on its own api`() {
        assertTrue(violations(":feature:home:impl", ":feature:home:api", ":data:pokemon:api").isEmpty())
    }

    @Test
    fun `coverage threshold applies to the expected modules only`() {
        val required = listOf(":core:common-kotlin", ":core:common-android", ":core:network:impl", ":data:pokemon:impl", ":feature:home:impl", ":debugtool")
        val notRequired = listOf(":core:designsystem", ":core:network:api", ":feature:home:api", ":core:testing", ":app")
        required.forEach { assertTrue(it, KoverConventionPlugin.requiresCoverageThreshold(it)) }
        notRequired.forEach { assertTrue(it, !KoverConventionPlugin.requiresCoverageThreshold(it)) }
    }
}

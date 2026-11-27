package com.khangtm.pokepoke.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Automated rules of docs/design/layers.md §5 marked with the robot: 3 and 11. */
class ViewModelRulesTest {

    private val viewModels: List<KoClassDeclaration> by lazy {
        Konsist.scopeFromProduction().classes().filter { it.name.endsWith(VIEW_MODEL_SUFFIX) }
    }

    @Test
    fun `scope finds the ViewModels of the repo`() {
        // Guards against a rule that passes only because Konsist read nothing.
        assertTrue("Konsist found no *ViewModel class", viewModels.isNotEmpty())
        assertTrue("Konsist found no *ViewModel under feature/", viewModels.any { it.isUnderFeature() })
    }

    @Test
    fun `rule 11 - no ViewModel knows Context Resources or Application`() {
        val violations = viewModels.flatMap { viewModel ->
            buildList {
                viewModel.containingFile.imports
                    .filter { it.name in FORBIDDEN_IMPORTS || (it.isWildcard && it.name.removeSuffix(".*") in FORBIDDEN_WILDCARD_PACKAGES) }
                    .forEach { add(viewModel.violation("imports ${it.name}")) }
                if (viewModel.parents().any { it.name.substringAfterLast(".") == "AndroidViewModel" }) {
                    add(viewModel.violation("extends AndroidViewModel"))
                }
                viewModel.constructorParameters()
                    .filter { parameter -> parameter.annotations.any { it.name == "ApplicationContext" } }
                    .forEach { add(viewModel.violation("has an @ApplicationContext parameter '${it.name}'")) }
            }
        }
        assertNoViolations("layers.md §5 rule 11: a ViewModel must not know Context (use UiText / UiMessage)", violations)
    }

    @Test
    fun `rule 3 - a feature ViewModel takes only UseCase Navigator or Assisted parameters`() {
        val violations = viewModels.filter { it.isUnderFeature() }.flatMap { viewModel ->
            viewModel.constructorParameters()
                .filterNot { parameter ->
                    parameter.type.name.endsWith(USE_CASE_SUFFIX) ||
                        parameter.type.name == NAVIGATOR ||
                        parameter.annotations.any { it.name == "Assisted" }
                }
                .map { viewModel.violation("constructor parameter '${it.name}: ${it.type.name}' is not a *UseCase, Navigator or @Assisted") }
        }
        assertNoViolations("layers.md §5 rule 3: a feature ViewModel may only inject use cases and Navigator", violations)
    }

    private fun KoClassDeclaration.constructorParameters() =
        (listOfNotNull(primaryConstructor) + constructors).flatMap { it.parameters }.distinct()

    private fun KoClassDeclaration.isUnderFeature() = projectPath.startsWith("/feature/")

    private fun KoClassDeclaration.violation(reason: String) = "$name ($projectPath): $reason"

    private fun assertNoViolations(rule: String, violations: List<String>) {
        if (violations.isNotEmpty()) fail("$rule\n" + violations.joinToString("\n") { "  - $it" })
    }

    private companion object {
        const val VIEW_MODEL_SUFFIX = "ViewModel"
        const val USE_CASE_SUFFIX = "UseCase"
        const val NAVIGATOR = "Navigator"
        val FORBIDDEN_IMPORTS = setOf(
            "android.content.Context",
            "android.content.res.Resources",
            "android.app.Application",
        )
        val FORBIDDEN_WILDCARD_PACKAGES = setOf("android.content", "android.content.res", "android.app")
    }
}

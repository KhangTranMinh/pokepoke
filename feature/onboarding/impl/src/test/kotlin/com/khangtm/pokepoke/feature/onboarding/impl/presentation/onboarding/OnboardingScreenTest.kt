package com.khangtm.pokepoke.feature.onboarding.impl.presentation.onboarding

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.core.app.ApplicationProvider
import com.khangtm.pokepoke.ui.designsystem.PokeTheme
import com.khangtm.pokepoke.ui.translation.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingScreenTest {

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    @Test
    fun `first page shows its text, Next and Skip`() = runComposeUiTest {
        setContent { PokeTheme { OnboardingScreen(OnboardingSamples.firstPage, onAction = {}) } }

        onNodeWithText(string(R.string.onboarding_page1_title)).assertIsDisplayed()
        onNodeWithText(string(R.string.onboarding_page1_body)).assertIsDisplayed()
        onNodeWithText(string(R.string.onboarding_next)).assertIsDisplayed()
        onNodeWithText(string(R.string.onboarding_skip)).assertIsDisplayed()
    }

    @Test
    fun `second page shows its text`() = runComposeUiTest {
        setContent { PokeTheme { OnboardingScreen(OnboardingSamples.secondPage, onAction = {}) } }

        onNodeWithText(string(R.string.onboarding_page2_title)).assertIsDisplayed()
        onNodeWithText(string(R.string.onboarding_page2_body)).assertIsDisplayed()
    }

    @Test
    fun `last page shows Get started and hides Skip`() = runComposeUiTest {
        setContent { PokeTheme { OnboardingScreen(OnboardingSamples.lastPage, onAction = {}) } }

        onNodeWithText(string(R.string.onboarding_page3_title)).assertIsDisplayed()
        onNodeWithText(string(R.string.onboarding_page3_body)).assertIsDisplayed()
        onNodeWithText(string(R.string.onboarding_get_started)).assertIsDisplayed()
        onAllNodesWithText(string(R.string.onboarding_skip)).assertCountEquals(0)
        onAllNodesWithText(string(R.string.onboarding_next)).assertCountEquals(0)
    }

    @Test
    fun `clicking Next, Skip and Get started sends the matching action`() = runComposeUiTest {
        val actions = mutableListOf<OnboardingUiAction>()
        val state = mutableStateOf(OnboardingSamples.firstPage)
        setContent { PokeTheme { OnboardingScreen(state.value, onAction = { actions += it }) } }

        onNodeWithText(string(R.string.onboarding_next)).performClick()
        onNodeWithText(string(R.string.onboarding_skip)).performClick()
        state.value = OnboardingSamples.lastPage
        waitForIdle()
        onNodeWithText(string(R.string.onboarding_get_started)).performClick()

        assertEquals(
            listOf(OnboardingUiAction.Next, OnboardingUiAction.Skip, OnboardingUiAction.Next),
            actions.filterNot { it is OnboardingUiAction.PageChanged },
        )
    }

    @Test
    fun `skip is ignored while finishing`() = runComposeUiTest {
        val actions = mutableListOf<OnboardingUiAction>()
        setContent {
            PokeTheme { OnboardingScreen(OnboardingSamples.firstPage.copy(isFinishing = true), onAction = { actions += it }) }
        }

        onNodeWithText(string(R.string.onboarding_skip)).performClick()

        assertEquals(emptyList<OnboardingUiAction>(), actions.filterNot { it is OnboardingUiAction.PageChanged })
    }

    @Test
    fun `swiping to the next page reports the new page`() = runComposeUiTest {
        val actions = mutableListOf<OnboardingUiAction>()
        setContent { PokeTheme { OnboardingScreen(OnboardingSamples.firstPage, onAction = { actions += it }) } }

        onNodeWithText(string(R.string.onboarding_page1_title)).performTouchInput { swipeLeft() }
        waitForIdle()

        assertEquals(OnboardingUiAction.PageChanged(1), actions.last())
    }

    @Test
    fun `the pager follows the state when Next changes the page`() = runComposeUiTest {
        val state = mutableStateOf(OnboardingSamples.firstPage)
        setContent { PokeTheme { OnboardingScreen(state.value, onAction = {}) } }

        state.value = OnboardingSamples.secondPage
        waitForIdle()

        onNodeWithText(string(R.string.onboarding_page2_title)).assertIsDisplayed()
    }

    @Test
    fun `previews provide every sample state`() {
        assertEquals(4, OnboardingStateProvider().values.toList().size)
    }
}

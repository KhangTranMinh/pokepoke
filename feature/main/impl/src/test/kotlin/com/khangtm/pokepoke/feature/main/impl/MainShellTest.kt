package com.khangtm.pokepoke.feature.main.impl

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.assertCountEquals
import androidx.test.core.app.ApplicationProvider
import com.khangtm.pokepoke.core.designsystem.PokeTheme
import com.khangtm.pokepoke.core.navigation.FakeNavigator
import com.khangtm.pokepoke.core.navigation.NavRequest
import com.khangtm.pokepoke.translation.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainShellTest {

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    @Test
    fun `shows every tab and the content`() = runComposeUiTest {
        setContent { PokeTheme { ShellWithContent(FakeNavigator(listOf(HomeKey))) } }

        listOf(R.string.home_tab, R.string.favourite_tab, R.string.profile_tab).forEach {
            onNodeWithText(string(it)).assertIsDisplayed()
        }
        onNodeWithText(CONTENT_TEXT).assertIsDisplayed()
    }

    @Test
    fun `selected tab is the one of the first key in the back stack`() = runComposeUiTest {
        setContent { PokeTheme { ShellWithContent(FakeNavigator(listOf(FavouriteKey, DetailKey))) } }

        onNodeWithText(string(R.string.favourite_tab)).assertIsSelected()
        onNodeWithText(string(R.string.home_tab)).assertIsNotSelected()
        onNodeWithText(string(R.string.profile_tab)).assertIsNotSelected()
    }

    @Test
    fun `selecting a tab resets the back stack to that tab`() = runComposeUiTest {
        val navigator = FakeNavigator(listOf(HomeKey))
        setContent { PokeTheme { ShellWithContent(navigator) } }

        onNodeWithText(string(R.string.profile_tab)).performClick()

        assertEquals(listOf<NavRequest>(NavRequest.ResetTo(listOf(ProfileKey))), navigator.requests)
        onNodeWithText(string(R.string.profile_tab)).assertIsSelected()
    }

    @Test
    fun `selecting the current tab resets to its root`() = runComposeUiTest {
        val navigator = FakeNavigator(listOf(HomeKey, DetailKey))
        setContent { PokeTheme { ShellWithContent(navigator) } }

        onNodeWithText(string(R.string.home_tab)).performClick()

        assertEquals(listOf<NavRequest>(NavRequest.ResetTo(listOf(HomeKey))), navigator.requests)
    }

    @Test
    fun `tabs are hidden while the top key is full screen`() = runComposeUiTest {
        setContent { PokeTheme { ShellWithContent(FakeNavigator(listOf(HomeKey, FullScreenKey))) } }

        listOf(R.string.home_tab, R.string.favourite_tab, R.string.profile_tab).forEach {
            onAllNodesWithText(string(it)).assertCountEquals(0)
        }
        onNodeWithText(CONTENT_TEXT).assertIsDisplayed()
    }

    @Test
    fun `tabs are hidden when the root key is not a destination`() = runComposeUiTest {
        setContent { PokeTheme { ShellWithContent(FakeNavigator(listOf(FullScreenKey))) } }

        onAllNodesWithText(string(R.string.home_tab)).assertCountEquals(0)
        onNodeWithText(CONTENT_TEXT).assertIsDisplayed()
    }

    @Test
    fun `tabs come back when the full screen key is popped`() = runComposeUiTest {
        val navigator = FakeNavigator(listOf(HomeKey, FullScreenKey))
        setContent { PokeTheme { ShellWithContent(navigator) } }
        onAllNodesWithText(string(R.string.home_tab)).assertCountEquals(0)

        runOnIdle { navigator.goBack() }

        onNodeWithText(string(R.string.home_tab)).assertIsDisplayed()
    }

    @Test
    fun `an empty destination set shows only the content`() = runComposeUiTest {
        setContent { PokeTheme { MainShell(FakeNavigator(listOf(HomeKey)), emptySet()) { Text(CONTENT_TEXT) } } }

        onNodeWithText(CONTENT_TEXT).assertIsDisplayed()
    }
}

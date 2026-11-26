package com.khangtm.pokepoke

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.core.app.ApplicationProvider
import com.khangtm.pokepoke.core.designsystem.PokeIcons
import com.khangtm.pokepoke.core.navigation.FakeNavigator
import com.khangtm.pokepoke.core.navigation.NavRequest
import com.khangtm.pokepoke.core.navigation.TopLevelDestination
import com.khangtm.pokepoke.feature.home.api.HomeNavKey
import com.khangtm.pokepoke.navigation.HomePlaceholderInstaller
import com.khangtm.pokepoke.translation.R
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PokePokeAppTest {

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    private val homeTab = object : TopLevelDestination {
        override val key: NavKey = HomeNavKey
        override val order = 0
        override val label = R.string.home_tab
        override val icon = PokeIcons.Home
        override val selectedIcon = PokeIcons.HomeSelected
    }

    @Test
    fun `shows the entry registered for the top key`() = runComposeUiTest {
        setContent {
            PokePokeApp(FakeNavigator(listOf(HomeNavKey)), emptySet(), setOf(HomePlaceholderInstaller), onFinish = {})
        }

        onNodeWithText(string(R.string.app_home_placeholder)).assertIsDisplayed()
    }

    @Test
    fun `shows nothing and does not crash while the back stack is empty`() = runComposeUiTest {
        setContent {
            PokePokeApp(FakeNavigator(), setOf(homeTab), setOf(HomePlaceholderInstaller), onFinish = {})
        }

        onAllNodesWithText(string(R.string.app_home_placeholder)).assertCountEquals(0)
    }

    @Test
    fun `shell shows the registered tabs around the entry and clicking a tab resets to it`() = runComposeUiTest {
        val navigator = FakeNavigator(listOf(HomeNavKey))
        setContent {
            PokePokeApp(navigator, setOf(homeTab), setOf(HomePlaceholderInstaller), onFinish = {})
        }

        onNodeWithText(string(R.string.home_tab)).assertIsSelected()
        onNodeWithText(string(R.string.home_tab)).performClick()

        assertEquals(listOf<NavRequest>(NavRequest.ResetTo(listOf(HomeNavKey))), navigator.requests)
        onNodeWithText(string(R.string.app_home_placeholder)).assertIsDisplayed()
    }
}

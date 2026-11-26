package com.khangtm.pokepoke.core.ui

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.core.app.ApplicationProvider
import com.khangtm.pokepoke.core.common.AppError
import com.khangtm.pokepoke.core.designsystem.PokeTheme
import com.khangtm.pokepoke.translation.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiComponentsTest {

    private fun string(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id, *args)

    // ---- ErrorState ----

    @Test
    fun `ErrorState shows title and message for every AppError`() {
        val cases = listOf(
            AppError.NoConnection to (R.string.ui_error_no_connection_title to string(R.string.ui_error_no_connection_message)),
            AppError.NotFound to (R.string.ui_error_not_found_title to string(R.string.ui_error_not_found_message)),
            AppError.Server(503) to (R.string.ui_error_server_title to string(R.string.ui_error_server_message, 503)),
            AppError.Unknown() to (R.string.ui_error_unknown_title to string(R.string.ui_error_unknown_message)),
        )
        cases.forEach { (error, expected) ->
            runComposeUiTest {
                setContent { PokeTheme { ErrorState(error = error, onRetry = {}) } }
                onNodeWithText(string(expected.first)).assertIsDisplayed()
                onNodeWithText(expected.second).assertIsDisplayed()
            }
        }
    }

    @Test
    fun `ErrorState retry calls onRetry`() = runComposeUiTest {
        var retries = 0
        setContent { PokeTheme { ErrorState(error = AppError.NoConnection, onRetry = { retries++ }) } }

        onNodeWithText(string(R.string.ui_retry)).performClick()
        assertEquals(1, retries)
    }

    @Test
    @Config(qualifiers = "vi")
    fun `ErrorState is localized in Vietnamese`() = runComposeUiTest {
        setContent { PokeTheme { ErrorState(error = AppError.Server(500), onRetry = {}) } }

        onNodeWithText("Lỗi máy chủ").assertIsDisplayed()
        onNodeWithText("Máy chủ gặp sự cố (500).").assertIsDisplayed()
        onNodeWithText("Thử lại").assertIsDisplayed()
    }

    // ---- EmptyState ----

    @Test
    fun `EmptyState shows action only when both text and callback are given`() = runComposeUiTest {
        var actions = 0
        var withAction by mutableStateOf(false)
        setContent {
            PokeTheme {
                EmptyState(
                    title = "Empty",
                    message = "Nothing",
                    actionText = "Do it",
                    onAction = if (withAction) ({ actions++ }) else null,
                )
            }
        }

        onNodeWithText("Empty").assertIsDisplayed()
        onNodeWithText("Nothing").assertIsDisplayed()
        onNodeWithText("Do it").assertDoesNotExist()
        withAction = true
        onNodeWithText("Do it").performClick()
        assertEquals(1, actions)
    }

    // ---- OfflineBanner / LoadingState ----

    @Test
    fun `OfflineBanner shows the offline text`() = runComposeUiTest {
        setContent { PokeTheme { OfflineBanner() } }
        onNodeWithText(string(R.string.ui_offline_banner)).assertIsDisplayed()
    }

    @Test
    fun `LoadingState shows a progress indicator`() = runComposeUiTest {
        setContent { PokeTheme { LoadingState() } }
        onNodeWithContentDescription(string(R.string.ui_loading)).assertIsDisplayed()
    }

    // ---- LoadMoreFooter ----

    @Test
    fun `LoadMoreFooter Idle and EndReached render nothing`() {
        listOf(LoadMoreState.Idle, LoadMoreState.EndReached).forEach { state ->
            runComposeUiTest {
                setContent { PokeTheme { LoadMoreFooter(state = state, onRetry = {}) } }
                onNodeWithContentDescription(string(R.string.ui_loading)).assertDoesNotExist()
                onNodeWithText(string(R.string.ui_load_more_error)).assertDoesNotExist()
                onNodeWithText(string(R.string.ui_retry)).assertDoesNotExist()
            }
        }
    }

    @Test
    fun `LoadMoreFooter Loading shows progress`() = runComposeUiTest {
        setContent { PokeTheme { LoadMoreFooter(state = LoadMoreState.Loading, onRetry = {}) } }
        onNodeWithContentDescription(string(R.string.ui_loading)).assertIsDisplayed()
    }

    @Test
    fun `LoadMoreFooter Error shows text and retry calls onRetry`() = runComposeUiTest {
        var retries = 0
        setContent {
            PokeTheme { LoadMoreFooter(state = LoadMoreState.Error(AppError.NoConnection), onRetry = { retries++ }) }
        }

        onNodeWithText(string(R.string.ui_load_more_error)).assertIsDisplayed()
        onNodeWithText(string(R.string.ui_retry)).performClick()
        assertEquals(1, retries)
    }

    // ---- MediaCard ----

    @Test
    fun `MediaCard shows texts, click and action work independently`() = runComposeUiTest {
        var clicks = 0
        var actions = 0
        setContent {
            PokeTheme {
                MediaCard(
                    title = "Bulbasaur",
                    overline = "#001",
                    imageUrl = null,
                    onClick = { clicks++ },
                    action = { PokeTextButtonForTest { actions++ } },
                )
            }
        }

        onNodeWithText("#001").assertIsDisplayed()
        onNodeWithText("Bulbasaur").performClick()
        assertEquals(1, clicks)
        onNodeWithText("action").performClick()
        assertEquals(1, actions)
    }

    // ---- MessageEffect ----

    @Test
    fun `MessageEffect shows the snackbar then calls onShown`() = runComposeUiTest {
        val host = SnackbarHostState()
        var shown = 0
        setContent {
            PokeTheme {
                SnackbarHost(host)
                MessageEffect(
                    message = UiMessage(R.string.ui_error_server_message, listOf(500), id = 1),
                    snackbarHostState = host,
                    onShown = { shown++ },
                )
            }
        }

        onNodeWithText(string(R.string.ui_error_server_message, 500)).assertIsDisplayed()
        assertEquals(0, shown)
        dismissSnackbarWithVirtualClock()
        assertEquals(1, shown)
        assertEquals(1, shown)
    }

    @Test
    fun `MessageEffect does nothing for a null message`() = runComposeUiTest {
        val host = SnackbarHostState()
        var shown = 0
        setContent {
            PokeTheme {
                SnackbarHost(host)
                MessageEffect(message = null, snackbarHostState = host, onShown = { shown++ })
            }
        }

        waitForIdle()
        assertEquals(0, shown)
        assertEquals(null, host.currentSnackbarData)
    }

    @Test
    fun `MessageEffect shows a new message with a new id`() = runComposeUiTest {
        val host = SnackbarHostState()
        var shown = 0
        var message by mutableStateOf<UiMessage?>(UiMessage(R.string.ui_retry, id = 1))
        setContent {
            PokeTheme {
                SnackbarHost(host)
                MessageEffect(message = message, snackbarHostState = host, onShown = { shown++ })
            }
        }
        dismissSnackbarWithVirtualClock()
        assertEquals(1, shown)

        message = UiMessage(R.string.ui_loading, id = 2)
        dismissSnackbarWithVirtualClock()
        assertEquals(2, shown)
    }
}

/** Advances the virtual Compose clock past SnackbarDuration.Short (4 s) so tests do not depend on machine speed. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.dismissSnackbarWithVirtualClock() {
    mainClock.autoAdvance = false
    waitForIdle()
    mainClock.advanceTimeBy(6_000)
    waitForIdle()
    mainClock.autoAdvance = true
    waitForIdle()
}

@androidx.compose.runtime.Composable
private fun PokeTextButtonForTest(onClick: () -> Unit) {
    com.khangtm.pokepoke.core.designsystem.PokeTextButton(text = "action", onClick = onClick)
}

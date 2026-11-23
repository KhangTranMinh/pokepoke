package com.khangtm.pokepoke.core.designsystem

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PokeComponentsTest {

    private fun string(id: Int): String = ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    private fun editableText(text: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(text))

    // ---- PokeTextField ----

    @Test
    fun `password is masked until show is pressed and masked again after hide`() = runComposeUiTest {
        setContent { PokeTheme { PokeTextField(value = "secret", onValueChange = {}, label = "Password", isPassword = true) } }

        onNode(hasSetTextAction()).assert(editableText("\u2022".repeat(6)))
        onNodeWithContentDescription(string(R.string.designsystem_show_password)).performClick()
        onNode(hasSetTextAction()).assert(editableText("secret"))
        onNodeWithContentDescription(string(R.string.designsystem_hide_password)).performClick()
        onNode(hasSetTextAction()).assert(editableText("\u2022".repeat(6)))
        onNodeWithContentDescription(string(R.string.designsystem_show_password)).assertIsDisplayed()
    }

    @Test
    fun `plain text field has no password toggle and shows its text`() = runComposeUiTest {
        setContent { PokeTheme { PokeTextField(value = "ash", onValueChange = {}, label = "Username") } }

        onNode(hasSetTextAction()).assert(editableText("ash"))
        onNodeWithContentDescription(string(R.string.designsystem_show_password)).assertDoesNotExist()
    }

    @Test
    fun `text field reports typed text and ime action`() = runComposeUiTest {
        var value by mutableStateOf("")
        var imeCount = 0
        setContent {
            PokeTheme {
                PokeTextField(value = value, onValueChange = { value = it }, label = "Username", onImeAction = { imeCount++ })
            }
        }

        onNode(hasSetTextAction()).performTextInput("misty")
        assertEquals("misty", value)
        onNode(hasSetTextAction()).performImeAction()
        assertEquals(1, imeCount)
    }

    @Test
    fun `text field shows supporting text`() = runComposeUiTest {
        setContent {
            PokeTheme {
                PokeTextField(value = "", onValueChange = {}, label = "Username", isError = true, supportingText = "Required")
            }
        }
        onNodeWithText("Required").assertIsDisplayed()
    }

    // ---- PokePrimaryButton ----

    @Test
    fun `primary button click calls onClick`() = runComposeUiTest {
        var clicks = 0
        setContent { PokeTheme { PokePrimaryButton(text = "Go", onClick = { clicks++ }) } }

        onNodeWithText("Go").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `primary button in loading state ignores clicks and shows progress instead of text`() = runComposeUiTest {
        var clicks = 0
        setContent { PokeTheme { PokePrimaryButton(text = "Go", onClick = { clicks++ }, loading = true) } }

        onNodeWithText("Go").assertDoesNotExist()
        onNodeWithContentDescription(string(R.string.designsystem_loading)).assertIsDisplayed().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun `primary button disabled ignores clicks`() = runComposeUiTest {
        var clicks = 0
        setContent { PokeTheme { PokePrimaryButton(text = "Go", onClick = { clicks++ }, enabled = false) } }

        onNodeWithText("Go").performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun `secondary button click calls onClick and loading ignores it`() = runComposeUiTest {
        var clicks = 0
        var loading by mutableStateOf(false)
        setContent { PokeTheme { PokeSecondaryButton(text = "Retry", onClick = { clicks++ }, loading = loading) } }

        onNodeWithText("Retry").performClick()
        assertEquals(1, clicks)
        loading = true
        onNodeWithContentDescription(string(R.string.designsystem_loading)).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `text button click calls onClick`() = runComposeUiTest {
        var clicks = 0
        setContent { PokeTheme { PokeTextButton(text = "More", onClick = { clicks++ }) } }

        onNodeWithText("More").performClick()
        assertEquals(1, clicks)
    }

    // ---- PokeSearchField ----

    @Test
    fun `search field shows clear only when query is not empty and clear calls onClear`() = runComposeUiTest {
        var query by mutableStateOf("")
        var clears = 0
        setContent {
            PokeTheme {
                PokeSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = {},
                    onClear = { clears++; query = "" },
                    placeholder = "Search",
                )
            }
        }

        onNodeWithContentDescription(string(R.string.designsystem_clear)).assertDoesNotExist()
        onNode(hasSetTextAction()).performTextInput("pika")
        onNodeWithContentDescription(string(R.string.designsystem_clear)).performClick()
        assertEquals(1, clears)
        onNodeWithContentDescription(string(R.string.designsystem_clear)).assertDoesNotExist()
    }

    @Test
    fun `search field ime search calls onSearch`() = runComposeUiTest {
        var searches = 0
        setContent {
            PokeTheme {
                PokeSearchField(query = "pika", onQueryChange = {}, onSearch = { searches++ }, onClear = {}, placeholder = "Search")
            }
        }

        onNode(hasSetTextAction()).performImeAction()
        assertEquals(1, searches)
    }

    // ---- PokeTopAppBar ----

    @Test
    fun `top app bar shows back only with onBack`() = runComposeUiTest {
        var backs = 0
        var withBack by mutableStateOf(false)
        setContent { PokeTheme { PokeTopAppBar(title = "Title", onBack = if (withBack) ({ backs++ }) else null) } }

        onNodeWithText("Title").assertIsDisplayed()
        onNodeWithContentDescription(string(R.string.designsystem_back)).assertDoesNotExist()
        withBack = true
        onNodeWithContentDescription(string(R.string.designsystem_back)).performClick()
        assertEquals(1, backs)
    }

    // ---- PokeCard / PokeListItem ----

    @Test
    fun `clickable card calls onClick`() = runComposeUiTest {
        var clicks = 0
        setContent { PokeTheme { PokeCard(onClick = { clicks++ }) { Text("Card") } } }

        onNodeWithText("Card").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `list item shows texts and calls onClick`() = runComposeUiTest {
        var clicks = 0
        setContent {
            PokeTheme { PokeListItem(headline = "Language", supporting = "English", onClick = { clicks++ }) }
        }

        onNodeWithText("English").assertIsDisplayed()
        onNodeWithText("Language").performClick()
        assertEquals(1, clicks)
    }

    // ---- Dialogs ----

    @Test
    fun `dialog confirm and dismiss call their callbacks`() = runComposeUiTest {
        var confirms = 0
        var dismisses = 0
        setContent {
            PokeTheme {
                PokeDialog(
                    title = "Title",
                    text = "Body",
                    confirmText = "OK",
                    onConfirm = { confirms++ },
                    dismissText = "No",
                    onDismiss = { dismisses++ },
                )
            }
        }

        onNodeWithText("Body").assertIsDisplayed()
        onNodeWithText("OK").performClick()
        onNodeWithText("No").performClick()
        assertEquals(1, confirms)
        assertEquals(1, dismisses)
    }

    @Test
    fun `radio dialog selects an option and cancel dismisses`() = runComposeUiTest {
        var selected: String? = null
        var dismisses = 0
        setContent {
            PokeTheme {
                PokeRadioDialog(
                    title = "Language",
                    options = listOf("en", "vi"),
                    selected = "en",
                    label = { "lang-$it" },
                    onSelect = { selected = it },
                    onDismiss = { dismisses++ },
                )
            }
        }

        onNodeWithText("lang-vi").performClick()
        assertEquals("vi", selected)
        onNodeWithText(string(R.string.designsystem_cancel)).performClick()
        assertEquals(1, dismisses)
    }
}

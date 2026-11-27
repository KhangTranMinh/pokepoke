package com.khangtm.pokepoke.ui.designsystem

import androidx.compose.ui.test.isDialog
import com.khangtm.pokepoke.core.testing.ScreenshotDevices
import com.khangtm.pokepoke.core.testing.runComposeScreenshotLightDark
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = ScreenshotDevices.PHONE)
class PokeScreenshotTest {

    @Test
    fun topAppBar() = runComposeScreenshotLightDark("poke_top_app_bar") { dark ->
        PokeTheme(dark) { PokeTopAppBarSample() }
    }

    @Test
    fun buttons() = runComposeScreenshotLightDark("poke_buttons") { dark ->
        PokeTheme(dark) { PokeButtonsSample() }
    }

    @Test
    fun textFields() = runComposeScreenshotLightDark("poke_text_fields") { dark ->
        PokeTheme(dark) { PokeTextFieldsSample() }
    }

    @Test
    fun searchField() = runComposeScreenshotLightDark("poke_search_field") { dark ->
        PokeTheme(dark) { PokeSearchFieldSample() }
    }

    @Test
    fun cardAndChip() = runComposeScreenshotLightDark("poke_card_and_chip") { dark ->
        PokeTheme(dark) { PokeCardAndChipSample() }
    }

    @Test
    fun listItem() = runComposeScreenshotLightDark("poke_list_item") { dark ->
        PokeTheme(dark) { PokeListItemSample() }
    }

    @Test
    fun dialog() = runComposeScreenshotLightDark("poke_dialog", target = { onNode(isDialog()) }) { dark ->
        PokeTheme(dark) { PokeDialogSample() }
    }

    @Test
    fun radioDialog() = runComposeScreenshotLightDark("poke_radio_dialog", target = { onNode(isDialog()) }) { dark ->
        PokeTheme(dark) { PokeRadioDialogSample() }
    }
}

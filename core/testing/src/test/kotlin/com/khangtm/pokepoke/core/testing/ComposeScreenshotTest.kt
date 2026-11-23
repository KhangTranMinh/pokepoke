package com.khangtm.pokepoke.core.testing

import androidx.compose.material3.Text
import androidx.compose.ui.test.SemanticsNodeInteraction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = ScreenshotDevices.PHONE)
class ComposeScreenshotTest {

    private val captured = mutableListOf<String>()
    private val recorder = ScreenshotCapturer { node: SemanticsNodeInteraction, path ->
        node.assertExists()
        captured += path
    }

    @Test
    fun `runComposeScreenshot composes content and captures name png`() {
        var composed = false
        runComposeScreenshot("sample", recorder) {
            composed = true
            Text("hello")
        }
        assertTrue(composed)
        assertEquals(listOf("sample.png"), captured)
    }

    @Test
    fun `runComposeScreenshotLightDark captures both themes in order`() {
        val themes = mutableListOf<Boolean>()
        runComposeScreenshotLightDark("sample", recorder) { dark ->
            themes += dark
            Text("hello")
        }
        assertEquals(listOf(false, true), themes)
        assertEquals(listOf("sample_light.png", "sample_dark.png"), captured)
    }

    @Test
    fun `device qualifiers are phone and tablet widths`() {
        assertTrue(ScreenshotDevices.PHONE.startsWith("w411dp"))
        assertTrue(ScreenshotDevices.TABLET.startsWith("w841dp"))
    }
}

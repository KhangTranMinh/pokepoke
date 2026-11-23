package com.khangtm.pokepoke.core.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import com.github.takahirom.roborazzi.captureRoboImage

/** Writes (record) or compares (verify) the image of [node] at [filePath]; no-op in plain unit test runs. */
fun interface ScreenshotCapturer {
    fun capture(node: SemanticsNodeInteraction, filePath: String)
}

/** Default capturer: Roborazzi. Relative paths resolve against the module's Roborazzi output dir. */
val RoborazziCapturer = ScreenshotCapturer { node, filePath -> node.captureRoboImage(filePath) }

/**
 * Composes [content] and captures the root as `<name>.png`.
 * Must run in a Robolectric test (`@RunWith(RobolectricTestRunner::class)`, `@GraphicsMode(NATIVE)`).
 * Wrap [content] in the app theme yourself; `:core:testing` does not depend on the design system.
 */
@OptIn(ExperimentalTestApi::class)
fun runComposeScreenshot(
    name: String,
    capturer: ScreenshotCapturer = RoborazziCapturer,
    content: @Composable () -> Unit,
) {
    runComposeUiTest {
        setContent(content)
        capturer.capture(onRoot(), "$name.png")
    }
}

/**
 * Captures [content] twice: `<name>_light.png` with `darkTheme = false` and `<name>_dark.png` with `darkTheme = true`.
 * [content] must pass `darkTheme` to the app theme.
 */
fun runComposeScreenshotLightDark(
    name: String,
    capturer: ScreenshotCapturer = RoborazziCapturer,
    content: @Composable (darkTheme: Boolean) -> Unit,
) {
    runComposeScreenshot("${name}_light", capturer) { content(false) }
    runComposeScreenshot("${name}_dark", capturer) { content(true) }
}

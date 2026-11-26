package com.khangtm.pokepoke.feature.main.impl

import com.khangtm.pokepoke.core.designsystem.PokeTheme
import com.khangtm.pokepoke.core.navigation.FakeNavigator
import com.khangtm.pokepoke.core.testing.ScreenshotDevices
import com.khangtm.pokepoke.core.testing.runComposeScreenshotLightDark
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainShellScreenshotTest {

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    fun bottomBarOnPhone() = runComposeScreenshotLightDark("main_shell_bottom_bar") { dark ->
        PokeTheme(dark) { ShellWithContent(FakeNavigator(listOf(HomeKey))) }
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.TABLET)
    fun navigationRailOnWideWindow() = runComposeScreenshotLightDark("main_shell_rail") { dark ->
        PokeTheme(dark) { ShellWithContent(FakeNavigator(listOf(FavouriteKey))) }
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    fun hiddenWhenFullScreenOnPhone() = runComposeScreenshotLightDark("main_shell_full_screen") { dark ->
        PokeTheme(dark) { ShellWithContent(FakeNavigator(listOf(HomeKey, FullScreenKey))) }
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.TABLET)
    fun hiddenWhenFullScreenOnWideWindow() = runComposeScreenshotLightDark("main_shell_full_screen_wide") { dark ->
        PokeTheme(dark) { ShellWithContent(FakeNavigator(listOf(HomeKey, FullScreenKey))) }
    }
}

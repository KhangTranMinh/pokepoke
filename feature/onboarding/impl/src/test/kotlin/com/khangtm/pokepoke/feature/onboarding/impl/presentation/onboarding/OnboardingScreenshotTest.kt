package com.khangtm.pokepoke.feature.onboarding.impl.presentation.onboarding

import com.khangtm.pokepoke.core.designsystem.PokeTheme
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
class OnboardingScreenshotTest {

    @Test
    fun firstPage() = runComposeScreenshotLightDark("onboarding_page1") { dark ->
        PokeTheme(dark) { OnboardingScreen(OnboardingSamples.firstPage, onAction = {}) }
    }

    @Test
    fun secondPage() = runComposeScreenshotLightDark("onboarding_page2") { dark ->
        PokeTheme(dark) { OnboardingScreen(OnboardingSamples.secondPage, onAction = {}) }
    }

    @Test
    fun lastPage() = runComposeScreenshotLightDark("onboarding_page3") { dark ->
        PokeTheme(dark) { OnboardingScreen(OnboardingSamples.lastPage, onAction = {}) }
    }

    @Test
    fun finishing() = runComposeScreenshotLightDark("onboarding_finishing") { dark ->
        PokeTheme(dark) { OnboardingScreen(OnboardingSamples.finishing, onAction = {}) }
    }
}

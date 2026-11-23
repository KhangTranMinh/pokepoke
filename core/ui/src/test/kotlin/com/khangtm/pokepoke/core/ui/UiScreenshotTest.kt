package com.khangtm.pokepoke.core.ui

import com.khangtm.pokepoke.core.common.AppError
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
class UiScreenshotTest {

    @Test
    fun remoteImage() = runComposeScreenshotLightDark("remote_image") { dark ->
        PokeTheme(dark) { RemoteImageSample() }
    }

    @Test
    fun loadingState() = runComposeScreenshotLightDark("loading_state") { dark ->
        PokeTheme(dark) { LoadingStateSample() }
    }

    @Test
    fun errorStateNoConnection() = runComposeScreenshotLightDark("error_state_no_connection") { dark ->
        PokeTheme(dark) { ErrorStateSample(AppError.NoConnection) }
    }

    @Test
    fun errorStateNotFound() = runComposeScreenshotLightDark("error_state_not_found") { dark ->
        PokeTheme(dark) { ErrorStateSample(AppError.NotFound) }
    }

    @Test
    fun errorStateServer() = runComposeScreenshotLightDark("error_state_server") { dark ->
        PokeTheme(dark) { ErrorStateSample(AppError.Server(503)) }
    }

    @Test
    fun errorStateUnknown() = runComposeScreenshotLightDark("error_state_unknown") { dark ->
        PokeTheme(dark) { ErrorStateSample(AppError.Unknown()) }
    }

    @Test
    fun emptyState() = runComposeScreenshotLightDark("empty_state") { dark ->
        PokeTheme(dark) { EmptyStateSample() }
    }

    @Test
    fun emptyStateMinimal() = runComposeScreenshotLightDark("empty_state_minimal") { dark ->
        PokeTheme(dark) { EmptyStateMinimalSample() }
    }

    @Test
    fun offlineBanner() = runComposeScreenshotLightDark("offline_banner") { dark ->
        PokeTheme(dark) { OfflineBannerSample() }
    }

    @Test
    fun loadMoreFooter() = runComposeScreenshotLightDark("load_more_footer") { dark ->
        PokeTheme(dark) { LoadMoreFooterSample() }
    }

    @Test
    fun mediaCard() = runComposeScreenshotLightDark("media_card") { dark ->
        PokeTheme(dark) { MediaCardSample() }
    }
}

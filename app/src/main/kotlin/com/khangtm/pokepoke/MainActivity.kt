package com.khangtm.pokepoke

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.khangtm.pokepoke.core.deeplink.DeepLinkDispatcher
import com.khangtm.pokepoke.core.navigation.EntryProviderInstaller
import com.khangtm.pokepoke.core.navigation.NavRequest
import com.khangtm.pokepoke.core.navigation.Navigator
import com.khangtm.pokepoke.core.navigation.TopLevelDestination
import com.khangtm.pokepoke.feature.home.api.HomeNavKey
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** AppCompatActivity: needed to change the app language on Android 11-12 (docs/design/screens.md §1.1). */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var navigator: Navigator
    @Inject lateinit var deepLinkDispatcher: DeepLinkDispatcher
    @Inject lateinit var destinations: Set<@JvmSuppressWildcards TopLevelDestination>
    @Inject lateinit var installers: Set<@JvmSuppressWildcards EntryProviderInstaller>

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState) // Hilt injects here
        splash.setKeepOnScreenCondition { navigator.backStack.isEmpty() }
        enableEdgeToEdge()

        // The stack lives in the activity-retained component: it is empty on a cold start and after process death.
        // The deep link is only read from the launch intent, never again when the activity is restored.
        if (navigator.backStack.isEmpty()) {
            val uri = if (savedInstanceState == null) intent.data?.toString() else null
            if (uri == null || !deepLinkDispatcher.dispatch(uri)) {
                navigator.execute(NavRequest.ResetTo(listOf(HomeNavKey)))
            }
        }

        setContent {
            PokePokeApp(navigator, destinations, installers, onFinish = ::finish)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // App already running: an unknown link leaves the current screen as it is.
        intent.data?.let { deepLinkDispatcher.dispatch(it.toString()) }
    }
}

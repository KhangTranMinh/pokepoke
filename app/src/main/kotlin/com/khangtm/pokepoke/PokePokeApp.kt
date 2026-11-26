package com.khangtm.pokepoke

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.khangtm.pokepoke.core.designsystem.PokeTheme
import com.khangtm.pokepoke.core.navigation.EntryProviderInstaller
import com.khangtm.pokepoke.core.navigation.Navigator
import com.khangtm.pokepoke.core.navigation.TopLevelDestination
import com.khangtm.pokepoke.feature.main.impl.MainShell

/**
 * Root composable of [MainActivity]: theme, shell (bottom bar / rail) and the `NavDisplay` showing
 * [Navigator.backStack] with the entries the features register (docs/design/screens.md §1.1).
 * [onFinish] is called when back is pressed with nothing left to pop.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun PokePokeApp(
    navigator: Navigator,
    destinations: Set<TopLevelDestination>,
    installers: Set<EntryProviderInstaller>,
    onFinish: () -> Unit,
) {
    PokeTheme {
        MainShell(navigator, destinations) {
            // The splash screen holds until the first request is applied; NavDisplay needs a non-empty stack.
            if (navigator.backStack.isEmpty()) return@MainShell
            val entries = remember(installers) {
                entryProvider<NavKey> { installers.forEach { it.install(this) } }
            }
            NavDisplay(
                backStack = navigator.backStack,
                onBack = { if (!navigator.goBack()) onFinish() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                sceneStrategies = listOf(rememberListDetailSceneStrategy()),
                entryProvider = entries,
            )
        }
    }
}

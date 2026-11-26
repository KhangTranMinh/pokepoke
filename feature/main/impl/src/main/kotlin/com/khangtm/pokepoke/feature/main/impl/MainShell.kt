package com.khangtm.pokepoke.feature.main.impl

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.khangtm.pokepoke.core.navigation.FullScreen
import com.khangtm.pokepoke.core.navigation.NavRequest
import com.khangtm.pokepoke.core.navigation.Navigator
import com.khangtm.pokepoke.core.navigation.TopLevelDestination

/**
 * App shell (docs/design/screens.md §1.2): a bottom bar on phones, a navigation rail on wide windows
 * (`NavigationSuiteScaffold` picks by window size), around [content] (the `NavDisplay`).
 *
 * - The selected tab is the destination whose key is the first key of the back stack.
 * - Clicking a tab resets the stack to that tab's root (also when it is already selected).
 * - The bar is hidden while the top key is [FullScreen], or when the root key is no destination (e.g. onboarding).
 */
@Composable
fun MainShell(
    navigator: Navigator,
    destinations: Set<TopLevelDestination>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val sorted = remember(destinations) { destinations.sortedBy { it.order } }
    val backStack = navigator.backStack
    val selected = sorted.firstOrNull { it.key == backStack.firstOrNull() }
    val showNavigation = selected != null && backStack.lastOrNull() !is FullScreen
    val navigationType = if (showNavigation) {
        NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfoV2())
    } else {
        NavigationSuiteType.None
    }

    NavigationSuiteScaffold(
        navigationItems = {
            sorted.forEach { destination ->
                val isSelected = destination == selected
                NavigationSuiteItem(
                    selected = isSelected,
                    onClick = { navigator.execute(NavRequest.ResetTo(listOf(destination.key))) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.icon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(destination.label)) },
                    navigationSuiteType = navigationType,
                )
            }
        },
        modifier = modifier,
        navigationSuiteType = navigationType,
        content = content,
    )
}

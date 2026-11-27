package com.khangtm.pokepoke.feature.main.impl

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.khangtm.pokepoke.ui.designsystem.PokeIcons
import com.khangtm.pokepoke.core.navigation.FullScreen
import com.khangtm.pokepoke.core.navigation.Navigator
import com.khangtm.pokepoke.core.navigation.TopLevelDestination
import com.khangtm.pokepoke.ui.translation.R

data object HomeKey : NavKey
data object FavouriteKey : NavKey
data object ProfileKey : NavKey

/** A key that is not a tab and is shown full screen (like Onboarding / Login). */
data object FullScreenKey : NavKey, FullScreen

/** A key that is neither a tab nor full screen. */
data object DetailKey : NavKey

class TestDestination(
    override val key: NavKey,
    override val order: Int,
    @StringRes override val label: Int,
    override val icon: ImageVector,
    override val selectedIcon: ImageVector,
) : TopLevelDestination

val testDestinations: Set<TopLevelDestination> = setOf(
    // Deliberately not in order, to check the shell sorts by `order`.
    TestDestination(ProfileKey, 2, R.string.profile_tab, PokeIcons.Profile, PokeIcons.ProfileSelected),
    TestDestination(HomeKey, 0, R.string.home_tab, PokeIcons.Home, PokeIcons.HomeSelected),
    TestDestination(FavouriteKey, 1, R.string.favourite_tab, PokeIcons.Favourite, PokeIcons.FavouriteSelected),
)

const val CONTENT_TEXT = "content"

@Composable
fun ShellWithContent(navigator: Navigator, modifier: Modifier = Modifier) {
    MainShell(navigator, testDestinations, modifier) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(CONTENT_TEXT) }
    }
}

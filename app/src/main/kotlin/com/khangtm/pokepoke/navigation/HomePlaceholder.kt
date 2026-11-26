package com.khangtm.pokepoke.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import com.khangtm.pokepoke.core.navigation.EntryProviderInstaller
import com.khangtm.pokepoke.feature.home.api.HomeNavKey
import com.khangtm.pokepoke.translation.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

// TEMPORARY (T14): `:feature:home` has no screen yet, so the app registers a stand-in entry for `HomeNavKey`
// to have something to show after the splash. T17 DELETES this file (and the `app_home_placeholder` string)
// when `:feature:home:impl` contributes the real entry.

@Composable
internal fun HomePlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.app_home_placeholder))
    }
}

internal val HomePlaceholderInstaller = EntryProviderInstaller { scope ->
    scope.entry<HomeNavKey> { HomePlaceholder() }
}

@Module
@InstallIn(SingletonComponent::class)
internal object HomePlaceholderModule {

    @Provides
    @IntoSet
    fun provideHomePlaceholderInstaller(): EntryProviderInstaller = HomePlaceholderInstaller
}

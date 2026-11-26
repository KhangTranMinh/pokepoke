package com.khangtm.pokepoke.feature.onboarding.impl.navigation

import com.khangtm.pokepoke.core.navigation.EntryProviderInstaller
import com.khangtm.pokepoke.feature.onboarding.api.OnboardingNavKey
import com.khangtm.pokepoke.feature.onboarding.impl.presentation.onboarding.OnboardingRoute

internal val OnboardingEntryInstaller = EntryProviderInstaller { scope ->
    scope.entry<OnboardingNavKey> { key -> OnboardingRoute(pending = key.pending) }
}

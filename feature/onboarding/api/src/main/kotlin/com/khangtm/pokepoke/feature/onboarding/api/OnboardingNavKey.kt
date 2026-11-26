package com.khangtm.pokepoke.feature.onboarding.api

import androidx.navigation3.runtime.NavKey
import com.khangtm.pokepoke.core.navigation.FullScreen
import com.khangtm.pokepoke.core.navigation.NavRequest

/** First-launch introduction. [pending] is the request to continue with when the user finishes or skips. */
data class OnboardingNavKey(val pending: NavRequest) : NavKey, FullScreen

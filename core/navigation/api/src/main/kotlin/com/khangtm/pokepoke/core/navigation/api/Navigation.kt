package com.khangtm.pokepoke.core.navigation.api

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/** Key that needs a signed-in user. The auth interceptor (`:feature:auth`) handles it. */
interface RequiresAuth

/** Key shown full screen: the shell hides the bottom bar / navigation rail. */
interface FullScreen

sealed interface NavRequest {
    data class Push(val key: NavKey) : NavRequest

    /** Replaces the top key; on an empty stack it behaves like [Push]. */
    data class ReplaceTop(val key: NavKey) : NavRequest

    data class ResetTo(val keys: List<NavKey>) : NavRequest
}

interface Navigator {
    /** Current back stack (Compose state: reading it in a composition recomposes on change). */
    val backStack: List<NavKey>

    /** Runs the interceptors, then applies the request. Not suspending; requests are processed one at a time. */
    fun execute(request: NavRequest)

    /** Pops the top key without interceptors. Returns false when only ≤ 1 key is left (the activity finishes). */
    fun goBack(): Boolean
}

sealed interface InterceptResult {
    data object Proceed : InterceptResult
    data class Redirect(val request: NavRequest) : InterceptResult
}

interface NavigationInterceptor {
    /** Lower runs first. Onboarding = 0, Auth = 100. */
    val priority: Int

    /** [currentStack] is the stack before the request is applied. */
    suspend fun intercept(request: NavRequest, currentStack: List<NavKey>): InterceptResult
}

/** A feature registers its screens (Hilt `@IntoSet`). */
fun interface EntryProviderInstaller {
    fun install(scope: EntryProviderScope<NavKey>)
}

/** A top-level tab (Hilt `@IntoSet`). */
interface TopLevelDestination {
    val key: NavKey
    val order: Int

    @get:StringRes
    val label: Int
    val icon: ImageVector
    val selectedIcon: ImageVector
}

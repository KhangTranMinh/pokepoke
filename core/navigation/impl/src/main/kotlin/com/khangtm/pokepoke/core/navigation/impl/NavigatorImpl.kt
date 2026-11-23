package com.khangtm.pokepoke.core.navigation.impl

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavKey
import com.khangtm.pokepoke.core.navigation.api.InterceptResult
import com.khangtm.pokepoke.core.navigation.api.NavRequest
import com.khangtm.pokepoke.core.navigation.api.NavigationInterceptor
import com.khangtm.pokepoke.core.navigation.api.Navigator
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Back stack kept in memory for the lifetime of the retained activity component (survives configuration changes
 * and per-app language changes; a process death starts over).
 *
 * - Interceptors run by ascending `priority`; the **first** `Redirect` wins and the redirected request is **not**
 *   intercepted again (no loops). A request resumed later (e.g. after login) goes through [execute] again, so it is
 *   intercepted normally.
 * - Requests are processed one at a time (a [Mutex] in [scope], on `Main.immediate` in production).
 * - [goBack] is applied immediately on the calling (main) thread, without interceptors.
 */
@ActivityRetainedScoped
internal class NavigatorImpl(
    interceptors: Set<NavigationInterceptor>,
    private val scope: CoroutineScope,
) : Navigator {

    @Inject
    constructor(
        interceptors: Set<@JvmSuppressWildcards NavigationInterceptor>,
        lifecycle: ActivityRetainedLifecycle,
    ) : this(interceptors, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)) {
        lifecycle.addOnClearedListener { scope.cancel() }
    }

    private val sortedInterceptors = interceptors.sortedBy { it.priority }
    private val stack = mutableStateListOf<NavKey>()
    private val mutex = Mutex()

    override val backStack: List<NavKey> get() = stack

    override fun execute(request: NavRequest) {
        scope.launch {
            mutex.withLock { apply(intercept(request)) }
        }
    }

    override fun goBack(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    private suspend fun intercept(request: NavRequest): NavRequest {
        val currentStack = stack.toList()
        for (interceptor in sortedInterceptors) {
            when (val result = interceptor.intercept(request, currentStack)) {
                InterceptResult.Proceed -> Unit
                is InterceptResult.Redirect -> return result.request
            }
        }
        return request
    }

    /** One snapshot per request, so readers never see a half-applied stack (e.g. an empty one during `ResetTo`). */
    private fun apply(request: NavRequest) {
        Snapshot.withMutableSnapshot {
            when (request) {
                is NavRequest.Push -> stack.add(request.key)
                is NavRequest.ReplaceTop ->
                    if (stack.isEmpty()) stack.add(request.key) else stack[stack.lastIndex] = request.key
                is NavRequest.ResetTo -> {
                    stack.clear()
                    stack.addAll(request.keys)
                }
            }
        }
    }
}

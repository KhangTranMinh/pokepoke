package com.khangtm.pokepoke.core.navigation.api

import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey

/**
 * [Navigator] for tests: records every [requests] and [backCount], and applies requests directly, synchronously and
 * without interceptors.
 */
class FakeNavigator(initialStack: List<NavKey> = emptyList()) : Navigator {

    private val stack = mutableStateListOf<NavKey>().apply { addAll(initialStack) }

    private val recorded = mutableListOf<NavRequest>()

    override val backStack: List<NavKey> get() = stack

    /** Every request passed to [execute], in order. */
    val requests: List<NavRequest> get() = recorded

    /** Number of calls to [goBack]. */
    var backCount: Int = 0
        private set

    override fun execute(request: NavRequest) {
        recorded += request
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

    override fun goBack(): Boolean {
        backCount++
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
}

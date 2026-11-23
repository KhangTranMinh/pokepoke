package com.khangtm.pokepoke.core.testing

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher

/**
 * Test replacements for the dispatchers injected by Hilt (`@IoDispatcher`, `@DefaultDispatcher`).
 * All dispatchers share one [scheduler], so virtual time advances consistently across them.
 *
 * ```
 * val dispatchers = TestDispatchers()
 * @get:Rule val mainRule = MainDispatcherRule(dispatchers.main)
 * val repository = SomeRepositoryImpl(ioDispatcher = dispatchers.io)
 * ```
 */
class TestDispatchers(val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()) {
    val main: TestDispatcher = StandardTestDispatcher(scheduler, name = "main")
    val io: TestDispatcher = StandardTestDispatcher(scheduler, name = "io")
    val default: TestDispatcher = StandardTestDispatcher(scheduler, name = "default")
}

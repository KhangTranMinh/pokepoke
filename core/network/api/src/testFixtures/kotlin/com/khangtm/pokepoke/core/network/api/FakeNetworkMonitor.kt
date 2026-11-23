package com.khangtm.pokepoke.core.network.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [NetworkMonitor]; change [online] in a test to simulate going online / offline. */
class FakeNetworkMonitor(online: Boolean = true) : NetworkMonitor {
    private val state = MutableStateFlow(online)

    var online: Boolean
        get() = state.value
        set(value) {
            state.value = value
        }

    override val isOnline: Flow<Boolean> = state

    override fun isOnlineNow(): Boolean = state.value
}

package com.khangtm.pokepoke.core.connectivity

import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
    /** Emits the current state on collection, then every change. */
    val isOnline: Flow<Boolean>

    /** `NetworkCapabilities`: INTERNET && VALIDATED. */
    fun isOnlineNow(): Boolean
}

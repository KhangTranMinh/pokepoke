package com.khangtm.pokepoke.core.common

data class AppVersion(val name: String, val code: Long)

/** Information about the running app. Implementation reads `PackageManager`; bound by Hilt in `:core:common`. */
interface AppInfo {
    val version: AppVersion
}

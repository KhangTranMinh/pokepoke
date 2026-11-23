package com.khangtm.pokepoke.core.common

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class AppInfoImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AppInfo {
    override val version: AppVersion by lazy {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        AppVersion(name = info.versionName.orEmpty(), code = info.longVersionCode)
    }
}

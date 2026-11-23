package com.khangtm.pokepoke.core.common.android

import android.content.Context
import com.khangtm.pokepoke.core.common.AppInfo
import com.khangtm.pokepoke.core.common.AppVersion
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

package com.khangtm.pokepoke.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AppInfoImplTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun `reads version name and code from the package manager`() {
        val info = shadowOf(context.packageManager).getInternalMutablePackageInfo(context.packageName)
        info.versionName = "1.2.3"
        info.longVersionCode = 45L

        assertEquals(AppVersion(name = "1.2.3", code = 45L), AppInfoImpl(context).version)
    }

    @Test
    fun `missing version name becomes empty`() {
        val info = shadowOf(context.packageManager).getInternalMutablePackageInfo(context.packageName)
        info.versionName = null
        info.longVersionCode = 1L

        assertEquals(AppVersion(name = "", code = 1L), AppInfoImpl(context).version)
    }
}

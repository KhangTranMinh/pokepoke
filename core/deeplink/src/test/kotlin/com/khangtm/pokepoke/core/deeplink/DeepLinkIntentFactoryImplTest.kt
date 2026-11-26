package com.khangtm.pokepoke.core.deeplink

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class DeepLinkIntentFactoryImplTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val factory = DeepLinkIntentFactoryImpl(context)

    private fun savedIntent(pendingIntent: PendingIntent) = shadowOf(pendingIntent).savedIntent

    @Test
    fun `intent views the uri in a new task`() {
        val intent = savedIntent(factory.pendingIntent("pokepoke://pokemon/25", 1))
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("pokepoke://pokemon/25", intent.dataString)
        assertEquals(context.packageName, intent.`package` ?: intent.component?.packageName)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
    }

    @Test
    fun `uses the launch activity of the app when there is one`() {
        val component = ComponentName(context.packageName, "${context.packageName}.MainActivity")
        val info = ActivityInfo().apply {
            packageName = component.packageName
            name = component.className
            exported = true
        }
        val pm = shadowOf(context.packageManager)
        pm.addOrUpdateActivity(info)
        pm.addIntentFilterForActivity(
            component,
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
        )
        val intent = savedIntent(factory.pendingIntent("https://pokepoke.com/me", 2))
        assertEquals(component, intent.component)
        assertEquals("https://pokepoke.com/me", intent.dataString)
    }

    @Test
    fun `pending intent is immutable`() {
        val pendingIntent = factory.pendingIntent("pokepoke://home", 3)
        assertTrue(shadowOf(pendingIntent).flags and PendingIntent.FLAG_IMMUTABLE != 0)
        assertTrue(shadowOf(pendingIntent).flags and PendingIntent.FLAG_UPDATE_CURRENT != 0)
    }

    @Test
    fun `request code is passed through`() {
        assertEquals(42, shadowOf(factory.pendingIntent("pokepoke://home", 42)).requestCode)
    }
}

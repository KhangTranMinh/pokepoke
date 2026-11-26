package com.khangtm.pokepoke.core.deeplink

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class DeepLinkIntentFactoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeepLinkIntentFactory {

    override fun pendingIntent(uri: String, requestCode: Int): PendingIntent {
        val intent = (context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent().setPackage(context.packageName))
            .apply {
                action = Intent.ACTION_VIEW
                data = uri.toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}

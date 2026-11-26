package com.khangtm.pokepoke.core.deeplink

import android.app.PendingIntent
import com.khangtm.pokepoke.core.navigation.NavRequest

/** Normalized link: `pokepoke://pokemon/25` and `https://pokepoke.com/pokemon/25` both give segments `["pokemon","25"]`. */
data class DeepLink(val segments: List<String>, val query: Map<String, String>)

/** Provided by `:app`: `DeepLinkConfig(schemes = setOf("pokepoke"), hosts = setOf("pokepoke.com"))`. */
data class DeepLinkConfig(val schemes: Set<String>, val hosts: Set<String>)

/** A feature registers one (Hilt `@IntoSet`). Returns null when the link does not match. */
fun interface DeepLinkHandler {
    fun handle(link: DeepLink): NavRequest?
}

interface DeepLinkDispatcher {
    /** Parse, ask the first matching handler, then `navigator.execute(request)`. False for an invalid URI or when no handler matches. */
    fun dispatch(uri: String): Boolean
}

/** Pattern such as `pokemon/{id}`. [match] returns the parameters, or null when the link does not match (segment counts must be equal). */
class DeepLinkPattern(pattern: String) {

    private val parts: List<String> = pattern.lowercase().split('/').filter { it.isNotEmpty() }

    fun match(link: DeepLink): Map<String, String>? {
        if (link.segments.size != parts.size) return null
        val params = mutableMapOf<String, String>()
        parts.zip(link.segments).forEach { (part, segment) ->
            if (part.length > 2 && part.startsWith("{") && part.endsWith("}")) {
                params[part.substring(1, part.length - 1)] = segment
            } else if (part != segment.lowercase()) {
                return null
            }
        }
        return params
    }
}

/** PendingIntent that opens the app with a URI (push notification / debugtool). */
interface DeepLinkIntentFactory {
    fun pendingIntent(uri: String, requestCode: Int): PendingIntent
}

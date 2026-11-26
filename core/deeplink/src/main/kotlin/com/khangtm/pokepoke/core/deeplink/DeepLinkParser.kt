package com.khangtm.pokepoke.core.deeplink

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder

/** Turns a raw URI string into a [DeepLink]; null when the URI is invalid or not ours. Everything is lowercased. */
internal class DeepLinkParser(private val config: DeepLinkConfig) {

    private val schemes = config.schemes.map { it.lowercase() }.toSet()
    private val hosts = config.hosts.map { it.lowercase() }.toSet()

    fun parse(raw: String): DeepLink? {
        val uri = try {
            URI(raw.trim())
        } catch (_: URISyntaxException) {
            return null
        }
        if (uri.isOpaque) return null
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = (uri.host ?: uri.authority)?.lowercase()
        val pathSegments = uri.path.orEmpty().lowercase().split('/').filter { it.isNotEmpty() }
        val segments = when {
            scheme == "https" || scheme == "http" -> {
                if (host == null || host !in hosts) return null
                pathSegments
            }
            scheme in schemes -> listOfNotNull(host?.takeIf { it.isNotEmpty() }) + pathSegments
            else -> return null
        }
        return DeepLink(segments, parseQuery(uri.rawQuery))
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrEmpty()) return emptyMap()
        return rawQuery.split('&').filter { it.isNotEmpty() }.associate { pair ->
            val key = pair.substringBefore('=')
            val value = pair.substringAfter('=', "")
            URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(value, "UTF-8")
        }
    }
}

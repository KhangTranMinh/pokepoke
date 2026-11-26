package com.khangtm.pokepoke.core.deeplink

import com.khangtm.pokepoke.core.navigation.Navigator
import javax.inject.Inject

internal class DeepLinkDispatcherImpl @Inject constructor(
    config: DeepLinkConfig,
    private val handlers: Set<@JvmSuppressWildcards DeepLinkHandler>,
    private val navigator: Navigator,
) : DeepLinkDispatcher {

    private val parser = DeepLinkParser(config)

    override fun dispatch(uri: String): Boolean {
        val link = parser.parse(uri) ?: return false
        val request = handlers.firstNotNullOfOrNull { it.handle(link) } ?: return false
        navigator.execute(request)
        return true
    }
}

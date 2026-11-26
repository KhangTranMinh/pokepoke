package com.khangtm.pokepoke.core.deeplink

/** Records dispatched URIs; returns [result] for each. */
class FakeDeepLinkDispatcher(var result: Boolean = true) : DeepLinkDispatcher {

    val dispatched = mutableListOf<String>()

    override fun dispatch(uri: String): Boolean {
        dispatched += uri
        return result
    }
}

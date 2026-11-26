package com.khangtm.pokepoke.core.deeplink

import com.khangtm.pokepoke.core.navigation.FakeNavigator
import com.khangtm.pokepoke.core.navigation.NavRequest
import com.khangtm.pokepoke.core.navigation.TestNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkDispatcherImplTest {

    private val config = DeepLinkConfig(setOf("pokepoke"), setOf("pokepoke.com"))
    private val navigator = FakeNavigator()
    private val first = NavRequest.Push(TestNavKey(1))
    private val second = NavRequest.Push(TestNavKey(2))

    private fun dispatcher(vararg handlers: DeepLinkHandler) =
        DeepLinkDispatcherImpl(config, linkedSetOf(*handlers), navigator)

    @Test
    fun `executes the request of the first matching handler`() {
        val dispatcher = dispatcher(
            DeepLinkHandler { null },
            DeepLinkHandler { first },
            DeepLinkHandler { second },
        )
        assertTrue(dispatcher.dispatch("pokepoke://pokemon/25"))
        assertEquals(listOf<NavRequest>(first), navigator.requests)
    }

    @Test
    fun `later handlers are not asked once one matches`() {
        var asked = false
        val dispatcher = dispatcher(DeepLinkHandler { first }, DeepLinkHandler { asked = true; second })
        dispatcher.dispatch("pokepoke://home")
        assertFalse(asked)
    }

    @Test
    fun `returns false and does not navigate when no handler matches`() {
        val dispatcher = dispatcher(DeepLinkHandler { null })
        assertFalse(dispatcher.dispatch("pokepoke://unknown"))
        assertEquals(emptyList<NavRequest>(), navigator.requests)
    }

    @Test
    fun `returns false without handlers`() {
        assertFalse(dispatcher().dispatch("pokepoke://home"))
    }

    @Test
    fun `invalid uri never reaches a handler`() {
        var asked = false
        val dispatcher = dispatcher(DeepLinkHandler { asked = true; first })
        assertFalse(dispatcher.dispatch("https://example.com/pokemon/25"))
        assertFalse(dispatcher.dispatch("garbage uri"))
        assertFalse(asked)
        assertEquals(emptyList<NavRequest>(), navigator.requests)
    }

    @Test
    fun `handler receives the normalized link`() {
        var received: DeepLink? = null
        val dispatcher = dispatcher(DeepLinkHandler { received = it; first })
        dispatcher.dispatch("HTTPS://PokePoke.com/Pokemon/25/?from=push")
        assertEquals(DeepLink(listOf("pokemon", "25"), mapOf("from" to "push")), received)
    }

    @Test
    fun `fake dispatcher records uris`() {
        val fake = FakeDeepLinkDispatcher(result = false)
        assertFalse(fake.dispatch("pokepoke://home"))
        assertEquals(listOf("pokepoke://home"), fake.dispatched)
    }
}

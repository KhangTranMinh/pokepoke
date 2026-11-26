package com.khangtm.pokepoke.core.deeplink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkPatternTest {

    private fun link(vararg segments: String) = DeepLink(segments.toList(), emptyMap())

    @Test
    fun `matches literals and captures parameters`() {
        assertEquals(mapOf("id" to "25"), DeepLinkPattern("pokemon/{id}").match(link("pokemon", "25")))
    }

    @Test
    fun `literal pattern matches with empty params`() {
        assertEquals(emptyMap<String, String>(), DeepLinkPattern("favourite").match(link("favourite")))
    }

    @Test
    fun `different segment count does not match`() {
        assertNull(DeepLinkPattern("pokemon/{id}").match(link("pokemon")))
        assertNull(DeepLinkPattern("pokemon/{id}").match(link("pokemon", "25", "extra")))
    }

    @Test
    fun `different literal does not match`() {
        assertNull(DeepLinkPattern("pokemon/{id}").match(link("move", "25")))
    }

    @Test
    fun `literal comparison ignores case`() {
        assertEquals(mapOf("id" to "25"), DeepLinkPattern("Pokemon/{id}").match(link("POKEMON", "25")))
    }

    @Test
    fun `empty pattern matches only the empty link`() {
        assertEquals(emptyMap<String, String>(), DeepLinkPattern("").match(link()))
        assertNull(DeepLinkPattern("").match(link("home")))
    }

    @Test
    fun `captures several parameters`() {
        assertEquals(
            mapOf("a" to "1", "b" to "2"),
            DeepLinkPattern("x/{a}/y/{b}").match(link("x", "1", "y", "2")),
        )
    }
}

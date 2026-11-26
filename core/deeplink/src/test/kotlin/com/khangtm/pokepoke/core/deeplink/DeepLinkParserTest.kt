package com.khangtm.pokepoke.core.deeplink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkParserTest {

    private val parser = DeepLinkParser(DeepLinkConfig(setOf("pokepoke"), setOf("pokepoke.com")))

    private fun segments(uri: String) = parser.parse(uri)?.segments

    @Test
    fun `custom scheme uses host as first segment`() {
        assertEquals(listOf("pokemon", "25"), segments("pokepoke://pokemon/25"))
    }

    @Test
    fun `https with known host uses path segments`() {
        assertEquals(listOf("pokemon", "25"), segments("https://pokepoke.com/pokemon/25"))
    }

    @Test
    fun `http with known host is accepted`() {
        assertEquals(listOf("me"), segments("http://pokepoke.com/me"))
    }

    @Test
    fun `https with unknown host is rejected`() {
        assertNull(parser.parse("https://example.com/pokemon/25"))
    }

    @Test
    fun `unknown scheme is rejected`() {
        assertNull(parser.parse("ftp://pokemon/25"))
        assertNull(parser.parse("other://pokemon/25"))
    }

    @Test
    fun `trailing slash is ignored`() {
        assertEquals(listOf("pokemon", "25"), segments("pokepoke://pokemon/25/"))
        assertEquals(listOf("me"), segments("https://pokepoke.com/me/"))
    }

    @Test
    fun `root path gives no segments`() {
        assertEquals(emptyList<String>(), segments("https://pokepoke.com/"))
        assertEquals(emptyList<String>(), segments("https://pokepoke.com"))
    }

    @Test
    fun `empty segments are dropped`() {
        assertEquals(listOf("pokemon", "25"), segments("https://pokepoke.com//pokemon//25"))
    }

    @Test
    fun `scheme host and path are case insensitive`() {
        assertEquals(listOf("pokemon", "pikachu"), segments("POKEPOKE://Pokemon/Pikachu"))
        assertEquals(listOf("pokemon", "6"), segments("HTTPS://PokePoke.COM/POKEMON/6"))
    }

    @Test
    fun `query parameters are parsed and decoded`() {
        val link = parser.parse("pokepoke://pokemon/25?from=push&name=Mr%20Mime&flag")
        assertEquals(listOf("pokemon", "25"), link?.segments)
        assertEquals(mapOf("from" to "push", "name" to "Mr Mime", "flag" to ""), link?.query)
    }

    @Test
    fun `no query gives empty map`() {
        assertEquals(emptyMap<String, String>(), parser.parse("pokepoke://home")?.query)
    }

    @Test
    fun `malformed uri is rejected`() {
        assertNull(parser.parse("not a uri"))
        assertNull(parser.parse(""))
        assertNull(parser.parse("pokemon/25"))
        assertNull(parser.parse("pokepoke:home"))
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals(listOf("home"), segments("  pokepoke://home  "))
    }
}

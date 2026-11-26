package com.khangtm.pokepoke

import com.khangtm.pokepoke.di.APP_DEEP_LINK_CONFIG
import com.khangtm.pokepoke.di.AppModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppModuleTest {

    @Test
    fun `base url is the flavor BASE_URL and ends with a slash`() {
        val baseUrl = AppModule.provideBaseUrl()

        assertEquals(BuildConfig.BASE_URL, baseUrl)
        assertTrue(baseUrl.startsWith("https://"))
        assertTrue(baseUrl.endsWith("/"))
    }

    @Test
    fun `deep link config has the pokepoke scheme and the pokepoke com host`() {
        val config = AppModule.provideDeepLinkConfig()

        assertEquals(APP_DEEP_LINK_CONFIG, config)
        assertEquals(setOf("pokepoke"), config.schemes)
        assertEquals(setOf("pokepoke.com"), config.hosts)
    }
}

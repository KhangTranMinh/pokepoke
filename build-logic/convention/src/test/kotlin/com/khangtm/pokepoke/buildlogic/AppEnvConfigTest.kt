package com.khangtm.pokepoke.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppEnvConfigTest {

    private val emptySecrets = "[common]\n[dev]\n[qa]\n[prod]\n"

    private fun config(common: String = "", dev: String = "", qa: String = "", prod: String = "") =
        "[common]\n$common\n[dev]\n$dev\n[qa]\n$qa\n[prod]\n$prod\n"

    private fun load(config: String, example: String = emptySecrets) = loadEnvConfig(config, example)

    private fun failure(block: () -> Unit): String {
        try {
            block()
        } catch (e: EnvConfigException) {
            return e.message.orEmpty()
        }
        throw AssertionError("Expected EnvConfigException")
    }

    // --- merge + types ---

    @Test
    fun `flavor values are common plus the flavor overrides`() {
        val result = load(
            config(
                common = "BASE_URL = \"https://a/\"\nNAME = \"x\"",
                dev = "NAME = \"dev\"",
                qa = "NAME = \"qa\"",
                prod = "NAME = \"prod\"",
            ),
        )

        assertEquals(EnvValue.Str("https://a/"), result.values.getValue("dev").getValue("BASE_URL"))
        assertEquals(EnvValue.Str("dev"), result.values.getValue("dev").getValue("NAME"))
        assertEquals(EnvValue.Str("prod"), result.values.getValue("prod").getValue("NAME"))
    }

    @Test
    fun `types are inferred as String long and boolean`() {
        val result = load(config(common = "S = \"a\"\nN = 42\nB = true"))
        val dev = result.values.getValue("dev")

        assertEquals("String" to "\"a\"", dev.getValue("S").let { it.javaType to it.javaLiteral })
        assertEquals("long" to "42L", dev.getValue("N").let { it.javaType to it.javaLiteral })
        assertEquals("boolean" to "true", dev.getValue("B").let { it.javaType to it.javaLiteral })
    }

    @Test
    fun `string literal escapes quotes backslashes and non ascii`() {
        assertEquals("\"a\\\"b\\\\c\\u00e9\"", javaStringLiteral("a\"b\\cé"))
    }

    @Test
    fun `placeholders contain every config key as text and no secret`() {
        val result = load(
            config = config(common = "N = 7\nB = false", dev = "H = \"h\"", qa = "H = \"h\"", prod = "H = \"h\""),
            example = "[common]\nTOKEN = \"\"\n[dev]\n[qa]\n[prod]\n",
        )

        assertEquals(mapOf("N" to "7", "B" to "false", "H" to "h"), result.placeholders("dev"))
        assertFalse("TOKEN" in result.placeholders("prod"))
        assertEquals(setOf("TOKEN"), result.secretKeys)
    }

    @Test
    fun `the committed shape with per flavor deeplink keys loads`() {
        val result = load(
            config(
                common = "BASE_URL = \"https://example.test/api/\"",
                dev = "DEEPLINK_SCHEME = \"pokepoke-dev\"\nDEEPLINK_HOST = \"dev.pokepoke.com\"",
                qa = "DEEPLINK_SCHEME = \"pokepoke-qa\"\nDEEPLINK_HOST = \"qa.pokepoke.com\"",
                prod = "DEEPLINK_SCHEME = \"pokepoke\"\nDEEPLINK_HOST = \"pokepoke.com\"",
            ),
        )

        assertEquals("pokepoke-qa", result.placeholders("qa").getValue("DEEPLINK_SCHEME"))
        assertTrue(result.secretKeys.isEmpty())
    }

    // --- config.toml failures ---

    @Test
    fun `missing section fails`() {
        val message = failure { load("[common]\n[dev]\n[qa]\n") }

        assertTrue(message, "[prod]" in message && CONFIG_FILE in message)
    }

    @Test
    fun `extra section fails`() {
        val message = failure { load(config() + "[staging]\n") }

        assertTrue(message, "staging" in message)
    }

    @Test
    fun `key outside any section fails`() {
        val message = failure { load("TOP = 1\n" + config()) }

        assertTrue(message, "TOP" in message)
    }

    @Test
    fun `sub table fails`() {
        val message = failure { load(config(common = "[common.nested]\nA = 1")) }

        assertTrue(message, "nested" in message)
    }

    @Test
    fun `dotted key sub table fails`() {
        val message = failure { load(config(common = "A.B = 1")) }

        assertTrue(message, "bảng con" in message)
    }

    @Test
    fun `array fails`() {
        val message = failure { load(config(common = "A = [1, 2]")) }

        assertTrue(message, "'A'" in message)
    }

    @Test
    fun `float fails`() {
        val message = failure { load(config(common = "A = 1.5")) }

        assertTrue(message, "'A'" in message && "Double" in message)
    }

    @Test
    fun `date fails`() {
        val message = failure { load(config(common = "A = 2026-01-01")) }

        assertTrue(message, "'A'" in message)
    }

    @Test
    fun `override with a different type fails`() {
        val message = failure {
            load(config(common = "A = 1", dev = "A = \"x\"", qa = "A = 2", prod = "A = 3"))
        }

        assertTrue(message, "[dev]" in message && "'A'" in message && "khác kiểu" in message)
    }

    @Test
    fun `key set differing between flavors fails`() {
        val message = failure { load(config(dev = "ONLY_DEV = 1")) }

        assertTrue(message, "'qa'" in message && "ONLY_DEV" in message)
    }

    @Test
    fun `lower case key name fails`() {
        val message = failure { load(config(common = "base_url = \"x\"")) }

        assertTrue(message, "UPPER_SNAKE_CASE" in message)
    }

    @Test
    fun `key starting with a digit or having double underscore fails`() {
        failure { load(config(common = "1A = 1")) }
        failure { load(config(common = "A__B = 1")) }
    }

    @Test
    fun `syntax error fails with the file name`() {
        val message = failure { load("[common\n") }

        assertTrue(message, CONFIG_FILE in message && "cú pháp" in message)
    }

    // --- secrets.example.toml failures ---

    @Test
    fun `example with a non empty value fails`() {
        val message = failure { load(config(), "[common]\nTOKEN = \"real\"\n[dev]\n[qa]\n[prod]\n") }

        assertTrue(message, SECRETS_EXAMPLE_FILE in message && "TOKEN" in message)
    }

    @Test
    fun `example with a non string value fails`() {
        val message = failure { load(config(), "[common]\nTOKEN = 1\n[dev]\n[qa]\n[prod]\n") }

        assertTrue(message, "phải là chuỗi" in message)
    }

    @Test
    fun `example missing a section fails`() {
        val message = failure { load(config(), "[common]\n[dev]\n[qa]\n") }

        assertTrue(message, SECRETS_EXAMPLE_FILE in message && "[prod]" in message)
    }

    @Test
    fun `secret key also present in config fails`() {
        val message = failure {
            load(config(common = "TOKEN = \"x\""), "[common]\nTOKEN = \"\"\n[dev]\n[qa]\n[prod]\n")
        }

        assertTrue(message, "TOKEN" in message && "cả" in message)
    }

    @Test
    fun `secret key set differing between flavors fails`() {
        val message = failure { load(config(), "[common]\n[dev]\nTOKEN = \"\"\n[qa]\n[prod]\n") }

        assertTrue(message, SECRETS_EXAMPLE_FILE in message && "TOKEN" in message)
    }

    // --- secrets.toml (resolveSecrets) ---

    private val required = setOf("A_KEY", "B_KEY")

    private fun secrets(common: String = "", dev: String = "", qa: String = "", prod: String = "") =
        config(common, dev, qa, prod)

    @Test
    fun `secrets resolve for the requested flavor`() {
        val text = secrets(common = "A_KEY = \"a\"", dev = "B_KEY = \"bd\"", qa = "B_KEY = \"bq\"", prod = "B_KEY = \"bp\"")

        assertEquals(mapOf("A_KEY" to "a", "B_KEY" to "bq"), resolveSecrets("qa", required, text))
    }

    @Test
    fun `missing secrets file fails and names the keys`() {
        val message = failure { resolveSecrets("dev", required, "") }

        assertTrue(message, SECRETS_FILE in message && "A_KEY" in message && "dev" in message)
    }

    @Test
    fun `missing secret key fails naming key and flavor`() {
        val message = failure { resolveSecrets("prod", required, secrets(common = "A_KEY = \"a\"")) }

        assertEquals("Thiếu secret 'B_KEY' cho flavor 'prod' trong $SECRETS_FILE.", message)
    }

    @Test
    fun `empty secret value fails`() {
        val message = failure { resolveSecrets("dev", required, secrets(common = "A_KEY = \"a\"\nB_KEY = \"\"")) }

        assertTrue(message, "B_KEY" in message && "rỗng" in message)
    }

    @Test
    fun `extra secret key fails`() {
        val text = secrets(common = "A_KEY = \"a\"\nB_KEY = \"b\"\nEXTRA = \"e\"")
        val message = failure { resolveSecrets("dev", required, text) }

        assertTrue(message, "EXTRA" in message && "thừa" in message)
    }

    @Test
    fun `non string secret fails`() {
        val message = failure { resolveSecrets("dev", required, secrets(common = "A_KEY = 1\nB_KEY = \"b\"")) }

        assertTrue(message, "phải là chuỗi" in message)
    }
}

package com.khangtm.pokepoke.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Consistency checks for the string resources (plain JUnit: reads `src/main/res`). */
class TranslationResourcesTest {

    private val res = File("src/main/res")

    private val allowedPrefixes = listOf(
        "designsystem_", "ui_", "home_", "details_", "favourite_",
        "profile_", "auth_", "onboarding_", "main_", "app_",
    )

    private val placeholder = Regex("%(?:\\d+\\$)?[a-zA-Z]")

    private fun strings(path: String): Map<String, String> {
        val file = File(res, path)
        assertTrue("Missing $file", file.exists())
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        val result = linkedMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as Element
            val name = element.getAttribute("name")
            assertFalse("Duplicate key '$name' in $path", name in result)
            result[name] = element.textContent
        }
        return result
    }

    private val english get() = strings("values/strings.xml")
    private val vietnamese get() = strings("values-vi/strings.xml")
    private val nonTranslation get() = strings("values/non-translation.xml")

    @Test
    fun `English and Vietnamese define exactly the same keys`() {
        assertEquals(english.keys.sorted(), vietnamese.keys.sorted())
    }

    @Test
    fun `placeholders match between English and Vietnamese`() {
        val vi = vietnamese
        english.forEach { (key, en) ->
            val enPlaceholders = placeholder.findAll(en).map { it.value }.sorted().toList()
            val viPlaceholders = placeholder.findAll(vi.getValue(key)).map { it.value }.sorted().toList()
            assertEquals("Placeholders differ for '$key'", enPlaceholders, viPlaceholders)
        }
    }

    @Test
    fun `every key has an allowed module prefix`() {
        (english.keys + vietnamese.keys + nonTranslation.keys).forEach { key ->
            assertTrue(
                "Key '$key' must start with one of $allowedPrefixes",
                allowedPrefixes.any { key.startsWith(it) },
            )
        }
    }

    @Test
    fun `no key is defined both as translated and non-translation`() {
        val overlap = english.keys.intersect(nonTranslation.keys)
        assertTrue("Keys in both files: $overlap", overlap.isEmpty())
    }

    @Test
    fun `non-translation has no Vietnamese version`() {
        assertFalse(File(res, "values-vi/non-translation.xml").exists())
    }

    @Test
    fun `values are not blank`() {
        (english + vietnamese + nonTranslation).forEach { (key, value) ->
            assertTrue("Blank value for '$key'", value.isNotBlank())
        }
    }

    @Test
    fun `res only contains the expected files`() {
        val files = res.walkTopDown().filter { it.isFile }.map { it.relativeTo(res).path }.sorted().toList()
        assertEquals(
            listOf("values-vi/strings.xml", "values/non-translation.xml", "values/strings.xml"),
            files,
        )
    }
}

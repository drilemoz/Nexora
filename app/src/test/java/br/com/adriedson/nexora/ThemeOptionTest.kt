package br.com.adriedson.nexora

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeOptionTest {
    @Test fun defaultForMissingOrUnknownValue() {
        assertEquals(ThemeOption.SYSTEM, ThemeOption.fromKey(null))
        assertEquals(ThemeOption.SYSTEM, ThemeOption.fromKey("invalid"))
    }
    @Test fun savedKeysAreStableAndDistinct() {
        assertEquals(4, ThemeOption.entries.map { it.key }.distinct().size)
        for (option in ThemeOption.entries) assertEquals(option, ThemeOption.fromKey(option.key))
    }
    @Test fun amoledAndDarkAreDifferentModes() {
        assertNotEquals(ThemeOption.DARK, ThemeOption.AMOLED)
        assertTrue(ThemeOption.AMOLED.title.contains("AMOLED"))
    }
}

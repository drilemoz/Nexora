package br.com.adriedson.nexora

import org.junit.Assert.assertEquals
import org.junit.Test

class ThermalDisplayTest {
    @Test fun rawCodePreserved() {
        assertEquals("Android: severa (3)", ThermalDisplay.status("severa", 3))
        assertEquals("Android: indisponível", ThermalDisplay.status("indisponível", null))
    }
    @Test fun unavailableHeadroomNeverInvented() {
        assertEquals("indisponível neste aparelho", ThermalDisplay.headroom(null))
        assertEquals("indisponível neste aparelho", ThermalDisplay.headroom(Float.NaN))
    }
    @Test fun ramBarBoundaries() {
        assertEquals(0, ThermalDisplay.availableRamProgress(40, 0))
        assertEquals(0, ThermalDisplay.availableRamProgress(-3, 100))
        assertEquals(100, ThermalDisplay.availableRamProgress(110, 100))
        assertEquals(25, ThermalDisplay.availableRamProgress(250, 1000))
    }
}

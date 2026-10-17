package br.com.adriedson.nexora

import org.junit.Assert.*
import org.junit.Test
class ModelLoadProgressTest {
    @Test fun firstTimeHasNoPercentageOrEta() {
        assertNull(ModelLoadProgress.estimate(2_000, null).percentage)
        assertNull(ModelLoadProgress.estimate(2_000, null).remainingMs)
    }
    @Test fun knownPreviousLoadEstimatesRemainingTime() {
        val v=ModelLoadProgress.estimate(3_000, 10_000)
        assertEquals(30,v.percentage)
        assertEquals(7000L,v.remainingMs)
        assertFalse(v.exceeded)
    }
    @Test fun overrunDoesNotInventCompletedPercentage() {
        val v=ModelLoadProgress.estimate(12_000, 10_000)
        assertNull(v.percentage)
        assertTrue(v.exceeded)
    }
    @Test fun timeFormat() {
        assertEquals("0:09",ModelLoadProgress.clock(9_500))
        assertEquals("1:11",ModelLoadProgress.clock(71_000))
    }
}

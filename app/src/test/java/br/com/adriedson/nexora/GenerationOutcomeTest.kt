package br.com.adriedson.nexora

import org.junit.Assert.*
import org.junit.Test

class GenerationOutcomeTest {
    @Test fun nativeTerminationDistinct() {
        assertEquals(GenerationOutcome.EOG, GenerationOutcome.fromNative(1))
        assertEquals(GenerationOutcome.LIMIT, GenerationOutcome.fromNative(2))
        assertEquals(GenerationOutcome.DECODE_ERROR, GenerationOutcome.fromNative(3))
        assertEquals(GenerationOutcome.UNKNOWN, GenerationOutcome.fromNative(0))
        assertEquals(GenerationOutcome.UNKNOWN, GenerationOutcome.fromNative(null))
    }
    @Test fun limitNeverClaimsCompletion() {
        assertTrue(GenerationOutcome.LIMIT.status(512).contains("incompleta"))
        assertFalse(GenerationOutcome.UNKNOWN.status(512).contains("concluída"))
        assertTrue(GenerationOutcome.EOG.status(512).contains("concluída"))
    }
}

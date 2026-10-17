package br.com.adriedson.nexora

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelInferenceSettingsTest {
    @Test fun perfilEconomicoReduzCusto() {
        val eco=ModelInferenceSettings.economical()
        val balanced=ModelInferenceSettings.balanced()
        assertTrue(eco.contextSize<balanced.contextSize)
        assertTrue(eco.threads<balanced.threads)
        assertTrue(eco.maxTokens<balanced.maxTokens)
    }
    @Test fun padraoPreservaAlpha15() {
        val s=ModelInferenceSettings.balanced()
        assertEquals(4096,s.contextSize)
        assertEquals(4,s.threads)
        assertEquals(.3f,s.temperature)
        assertEquals(512,s.maxTokens)
    }
    @Test(expected=IllegalArgumentException::class) fun rejeitaContextoInvalido() {
        ModelInferenceSettings(contextSize=1234)
    }
    @Test(expected=IllegalArgumentException::class) fun rejeitaTemperaturaForaDoLimite() {
        ModelInferenceSettings(temperature=3f)
    }
}

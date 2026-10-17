package br.com.adriedson.nexora

import org.junit.Assert.*
import org.junit.Test

class ModelDisplayNamesTest {
    @Test fun qwen3FriendlyName() {
        assertEquals("Qwen 3 • 4B", ModelDisplayNames.defaultName("Qwen3-4B-Q4_K_M.gguf"))
        assertEquals("Qwen 3 • 1.7B", ModelDisplayNames.defaultName("Qwen_Qwen3-1.7B-Q4_K_M.gguf"))
    }
    @Test fun fallbackDoesNotChangeIdentity() {
        assertEquals("SmolLM2 360M Instruct", ModelDisplayNames.defaultName("SmolLM2-360M-Instruct-Q4_K_M.gguf"))
        assertEquals("Sem modelo", ModelDisplayNames.defaultName("Sem modelo"))
    }
    @Test fun validatesCustomAlias() {
        assertEquals("Meu modelo local", ModelDisplayNames.validCustomName("  Meu   modelo local  "))
        assertNull(ModelDisplayNames.validCustomName("   "))
        assertNull(ModelDisplayNames.validCustomName("x".repeat(49)))
    }
}

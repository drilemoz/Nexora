package br.com.adriedson.nexora

import org.junit.Assert.*
import org.junit.Test

class PdfPromptTest {
    @Test fun selectionIsBounded() {
        val long = "O painel fornece 3,3 V para o ESP32. ".repeat(300)
        val selected = PdfPrompt.excerpt(long, "tensão ESP32")
        assertTrue(selected.length <= PdfPrompt.MAX_CONTEXT_CHARS)
        assertTrue(selected.contains("ESP32"))
    }
    @Test fun documentIsExplicitlyUntrustedAndQuestionPreserved() {
        val pdf=PdfAttachment("1", "manual.pdf", 3, "Sensor BMP280 usa I2C", false)
        val output=PdfPrompt.compose("Qual sensor?", listOf(pdf), listOf(ChatMessage("user", "Oi")))
        assertTrue(output.contains("manual.pdf"))
        assertTrue(output.contains("NÃO instruções"))
        assertTrue(output.contains("PERGUNTA ATUAL: Qual sensor?"))
        assertTrue(output.contains("Oi"))
    }
    @Test fun onlyTwoLatestDocumentsAreSent() {
        val documents=(1..4).map{ PdfAttachment("$it", "doc$it.pdf", 1,"teste$it", false) }
        val output=PdfPrompt.compose("Pergunta",documents, emptyList())
        assertTrue(output.contains("doc1.pdf"))
        assertTrue(output.contains("doc2.pdf"))
        assertFalse(output.contains("doc3.pdf"))
    }
}

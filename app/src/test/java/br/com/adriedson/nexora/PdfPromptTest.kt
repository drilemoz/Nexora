package br.com.adriedson.nexora

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfPromptTest {
    @Test fun primeiraPerguntaSemAnexosNaoRecebeMoldura() {
        val question = "Um ciclista saiu às 8h: quando chegou?"
        assertEquals(question, PdfPrompt.compose(question, emptyList(), emptyList()))
    }

    @Test fun turnosAnterioresContinuamNoResumoQuandoContextoEhRestaurado() {
        val history = listOf(ChatMessage("user", "Pergunta anterior"), ChatMessage("assistant", "Resposta anterior"))
        val actual = PdfPrompt.compose("Nova pergunta", emptyList(), history)
        assertTrue(actual.contains("Pessoa: Pergunta anterior"))
        assertTrue(actual.contains("Assistente: Resposta anterior"))
        assertTrue(actual.endsWith("PERGUNTA ATUAL: Nova pergunta"))
    }

    @Test fun anexoContinuaLimitadoParaEvitarSobrecarga() {
        val pdf = PdfAttachment("1", "teste.pdf", 1, "texto local", false)
        val result = PdfPrompt.compose("Resumo?", listOf(pdf), emptyList())
        assertTrue(result.contains("DOCUMENTO ANEXADO"))
        assertTrue(result.contains("PERGUNTA ATUAL: Resumo?"))
    }
}

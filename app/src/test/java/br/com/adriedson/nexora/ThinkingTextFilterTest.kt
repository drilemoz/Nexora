package br.com.adriedson.nexora

import org.junit.Assert.assertEquals
import org.junit.Test

class ThinkingTextFilterTest {
    @Test fun hidesCompleteReasoningBlock() {
        assertEquals("Olá!", ThinkingTextFilter.visibleText("<think>Thinking in English</think>Olá!"))
    }
    @Test fun preservesPlainText() {
        assertEquals("Resposta direta", ThinkingTextFilter.visibleText("Resposta direta"))
    }
    @Test fun handlesSplitTagsDuringStreaming() {
        val f = ThinkingTextFilter()
        assertEquals("", f.append("<thi"))
        assertEquals("", f.append("nk>oculto</th"))
        assertEquals("Aqui", f.append("ink>Aqui"))
        assertEquals("Aqui está", f.append(" está"))
        assertEquals("Aqui está", f.finish())
    }
    @Test fun reasoningOnlyIsNotShown() {
        assertEquals("", ThinkingTextFilter.visibleText("<think>oculto</think>"))
    }
    @Test fun unusualTextOutsideBlockIsPreserved() {
        assertEquals("antesdepois", ThinkingTextFilter.visibleText("antes<think>oculto</think>depois"))
    }
}

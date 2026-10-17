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
    @Test fun incrementalMatchesCompleteAcrossEverySplit() {
        val samples = listOf(
            "ola <think>razao longa</think> resposta final",
            "<think>interno</think>Árvore",
            "texto simples com < etiqueta",
            "antes<think>oculto</think>depois<thi",
            "<think>nunca fechou",
            "áéíóú<not-tag>fim"
        )
        for (sample in samples) {
            for (split in 0..sample.length) {
                val f = ThinkingTextFilter()
                f.append(sample.substring(0, split))
                f.append(sample.substring(split))
                assertEquals("Falha em split=$split texto=$sample",
                    ThinkingTextFilter.visibleText(sample), f.finish())
            }
        }
    }
    @Test fun batchedUiRenderingMatchesFinishedResponse() {
        val f = ThinkingTextFilter()
        f.accept("<thi")
        assertEquals("", f.visibleText())
        f.accept("nk>segredo</think>Resposta")
        assertEquals("Resposta", f.visibleText())
        f.accept(" final")
        assertEquals("Resposta final", f.finish())
    }

    @Test fun unusualTextOutsideBlockIsPreserved() {
        assertEquals("antesdepois", ThinkingTextFilter.visibleText("antes<think>oculto</think>depois"))
    }
}

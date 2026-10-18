package br.com.adriedson.nexora

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelTemplateReportTest {
    @Test fun qwenSmollmIsNotAssumedSpecificFromMarkers() {
        val r = ModelTemplateReport.parse("ChatML_Qwen_SmolLM|gguf|ativo|verificado|ok|ea0c4c2c85fb")!!
        assertTrue(r.familyLabel.contains("Qwen/SmolLM"))
        assertTrue(r.applicationLabel.startsWith("Ativo"))
        assertTrue(r.compatibilityLabel.contains("tokenização"))
    }
    @Test fun incompatibleNeverBecomesApplied() {
        val r = ModelTemplateReport.parse("Gemma|gguf|incompativel|verificado|prefixo_multiturno|ea0c4c2c85fb")!!
        assertTrue(r.applicationLabel.contains("bloqueada"))
        assertTrue(r.compatibilityLabel.contains("diverge"))
    }
    @Test fun unsupportedSystemRoleIsVisibleAndNeverSilentlyDiscarded() {
        val r = ModelTemplateReport.parse("Gemma|gguf|ativo|verificado|ok_sem_system|ea0c4c2c85fb")!!
        assertTrue(r.compatibilityLabel.contains("não suporta o papel system"))
    }
    @Test fun missingMetadataNotClaimedApplied() {
        val r = ModelTemplateReport.parse("desconhecido|fallback|nao_aplicado|verificado|sem_template_gguf|ea0c4c2c85fb")!!
        assertEquals("Não aplicado", r.applicationLabel)
        assertTrue(r.originLabel.contains("Fallback"))
    }
    @Test fun rawTemplateAndMessageCannotEnterDiagnostic() {
        val r = ModelTemplateReport.parse("Phi|gguf|ativo|verificado|ok|ea0c4c2c85fb")!!
        val d = r.diagnostic("modelo.gguf")
        assertFalse(d.contains("/storage/"))
        assertFalse(d.contains("nx_assistant_probe"))
        assertFalse(d.contains("{{"))
        assertTrue(d.contains("modelo.gguf"))
    }
    @Test fun unknownOrMalformedStatusIsRejected() {
        assertNull(ModelTemplateReport.parse("Gemma|gguf|ativo"))
        assertNull(ModelTemplateReport.parse("Gemma|gguf|ativo|verificado|ok|/storage/emulated/0"))
        assertNull(ModelTemplateReport.parse("Gemma|gguf|false_positive|verificado|ok|ea0c4c2c85fb"))
        assertNull(ModelTemplateReport.parse("Gemma|gguf|ativo|verificado|ok|ea0c4c2c85fb|msg"))
    }
    @Test fun familiesDoNotChangeMode() {
        val all = listOf("Gemma","Llama","Phi","ChatML_Qwen_SmolLM","Jinja_GGUF")
        all.forEach {
            val x = ModelTemplateReport.parse("$it|gguf|ativo|verificado|ok|ea0c4c2c85fb")!!
            assertEquals("gguf", x.origin)
            assertTrue(x.familyLabel.isNotBlank())
        }
    }
}

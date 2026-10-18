package br.com.adriedson.nexora

/** Diagnóstico por enums vindos do JNI. Não recebe template bruto, texto ou caminho do GGUF. */
data class ModelTemplateReport(
    val family: String,
    val origin: String,
    val application: String,
    val verification: String,
    val error: String,
    val engine: String
) {
    val originLabel: String get() = when (origin) {
        "gguf" -> "Metadados GGUF (tokenizer.chat_template)"
        "manual" -> "Configuração manual"
        else -> "Fallback (sem template GGUF)"
    }
    val familyLabel: String get() = when(family) {
        "Gemma" -> "Gemma (marcadores reconhecidos)"
        "Llama" -> "Llama (marcadores reconhecidos)"
        "ChatML_Qwen_SmolLM" -> "ChatML (Qwen/SmolLM e compatíveis)"
        "Phi" -> "Phi (marcadores reconhecidos)"
        "Jinja_GGUF" -> "Template Jinja definido pelo GGUF"
        else -> "Não identificado"
    }
    val applicationLabel: String get() = when(application) {
        "ativo" -> "Ativo (formatação validada)"
        "incompativel" -> "Incompatível (aplicação bloqueada)"
        "nao_aplicado" -> "Não aplicado"
        else -> "Não verificado"
    }
    val compatibilityLabel: String get() = when(error) {
        "ok" -> "Formatou system/user/assistant e múltiplos turnos; tokenização sintética aprovada."
        "ok_sem_system" -> "Texto multiturno aprovado, mas o GGUF não suporta o papel system. Mensagens system serão bloqueadas."
        "sem_template_gguf" -> "Este GGUF não declarou template; o motor conserva o modo legado, sem aplicação comprovada."
        "prefixo_multiturno" -> "O formato incremental diverge no histórico. Para evitar corromper a conversa, a geração foi bloqueada."
        "tokenizacao_vazia" -> "Template gerado, mas tokenização sintética falhou."
        "inicializacao_template", "template_incompativel" -> "Template não suportado por esta revisão do motor."
        "motor_indisponivel" -> "Motor não disponível para inspeção."
        else -> "Ainda não verificado no motor."
    }
    fun diagnostic(filename: String): String = buildString {
        appendLine("Nexora • Compatibilidade dos modelos")
        appendLine("Modelo GGUF: ${filename.replace(Regex("[\r\n\t]"), " ")}")
        appendLine("Template: $familyLabel")
        appendLine("Origem: $originLabel")
        appendLine("Aplicação: $applicationLabel")
        appendLine("Verificação: $verification")
        appendLine("Resultado: $compatibilityLabel")
        appendLine("llama.cpp: ${engine.take(20)} (revisão fixada)")
        appendLine("Raciocínio: parâmetros originais do template preservados; saída não gerada no teste")
        appendLine("Teste: formatação e tokenização sintéticas, sem inferência")
        append("Não contém caminhos locais, conversas, texto de PDF nem conteúdo do template")
    }
    companion object {
        fun parse(raw: String): ModelTemplateReport? {
            val f = raw.split('|')
            if (f.size != 6 || f.any { it.length > 40 || !it.matches(Regex("[a-zA-Z0-9_]+")) }) return null
            if (f[2] !in setOf("ativo", "incompativel", "nao_aplicado", "nao_verificado")) return null
            if (f[1] !in setOf("gguf", "manual", "fallback")) return null
            return ModelTemplateReport(f[0],f[1],f[2],f[3],f[4],f[5])
        }
    }
}

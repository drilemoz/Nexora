package br.com.adriedson.nexora

/** Resultados explicitamente emitidos pelo JNI; sem inferir motivo pela contagem de fragmentos. */
enum class GenerationOutcome(val diagnostic: String) {
    EOG("eog"), LIMIT("limit"), DECODE_ERROR("decode_error"), UNKNOWN("unknown");

    fun status(tokenLimit: Int): String = when (this) {
        EOG -> "Resposta concluída • sem conexão com servidores"
        LIMIT -> "Limite de $tokenLimit tokens atingido • resposta pode estar incompleta"
        DECODE_ERROR -> "Erro nativo durante a geração • resposta parcial"
        UNKNOWN -> "Geração encerrada • conclusão não confirmada"
    }

    companion object {
        fun fromNative(code: Int?): GenerationOutcome = when (code) {
            1 -> EOG
            2 -> LIMIT
            3 -> DECODE_ERROR
            else -> UNKNOWN
        }
    }
}

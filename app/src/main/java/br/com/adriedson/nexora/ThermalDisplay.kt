package br.com.adriedson.nexora

import java.util.Locale

/** Interpretação apenas de rótulos. Nunca estima a temperatura da CPU. */
object ThermalDisplay {
    fun status(text: String, raw: Int?): String =
        if (raw == null) "Android: indisponível" else "Android: $text ($raw)"

    fun headroom(value: Float?): String =
        value?.takeIf { it.isFinite() && it >= 0f }?.let {
            String.format(Locale.forLanguageTag("pt-BR"), "%.2f", it) + " (1,00 = limiar severo)"
        } ?: "indisponível neste aparelho"

    fun availableRamProgress(available: Long, total: Long): Int =
        if (total <= 0) 0 else ((available.coerceIn(0, total) * 100.0) / total).toInt().coerceIn(0, 100)
}

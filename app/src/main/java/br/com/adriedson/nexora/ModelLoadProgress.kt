package br.com.adriedson.nexora

/** Sem porcentagem nativa do llama.cpp: apenas uma estimativa baseada no tempo da carga anterior. */
data class LoadEstimate(val percentage: Int?, val remainingMs: Long?, val exceeded: Boolean)
object ModelLoadProgress {
    fun estimate(elapsedMs: Long, referenceMs: Long?): LoadEstimate {
        if (referenceMs == null || referenceMs < 100L || elapsedMs < 0L) return LoadEstimate(null, null, false)
        if (elapsedMs >= referenceMs) return LoadEstimate(null, null, true)
        val percent = (elapsedMs.toDouble() / referenceMs * 100.0).toInt().coerceIn(0, 95)
        return LoadEstimate(percent, referenceMs - elapsedMs, false)
    }
    fun clock(milliseconds: Long): String {
        val seconds = (milliseconds.coerceAtLeast(0L) / 1000L)
        return "%d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
    }
}

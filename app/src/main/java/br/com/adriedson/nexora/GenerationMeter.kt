package br.com.adriedson.nexora

/** Mede fragmentos emitidos pelo motor local. Não confundir com um benchmark nativo preciso. */
class GenerationMeter {
    private var startedNs: Long = 0L
    private var firstNs: Long = -1L
    private var lastNs: Long = -1L
    private var fragments: Int = 0

    fun start(nowNs: Long) {
        require(nowNs >= 0L)
        startedNs = nowNs
        firstNs = -1L
        lastNs = -1L
        fragments = 0
    }

    fun onFragment(fragment: String, nowNs: Long) {
        if (fragment.isEmpty() || nowNs < startedNs) return
        if (firstNs < 0L) firstNs = nowNs
        lastNs = nowNs
        fragments++
    }

    fun reading(nowNs: Long): Reading {
        val first = firstNs.takeIf { it >= 0L }
        val last = lastNs.takeIf { it >= 0L }
        val responseSeconds = first?.let { (it - startedNs).coerceAtLeast(0L) / 1e9 }
        val interval = if (first != null && last != null) (last - first).coerceAtLeast(0L) / 1e9 else 0.0
        val speed = if (fragments > 1 && interval > 0.0) (fragments - 1) / interval else null
        return Reading(
            fragments = fragments,
            timeToFirstSeconds = responseSeconds,
            fragmentsPerSecond = speed,
            elapsedSeconds = (nowNs - startedNs).coerceAtLeast(0L) / 1e9
        )
    }

    data class Reading(
        val fragments: Int,
        val timeToFirstSeconds: Double?,
        val fragmentsPerSecond: Double?,
        val elapsedSeconds: Double
    )
}

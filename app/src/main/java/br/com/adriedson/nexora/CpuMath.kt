package br.com.adriedson.nexora

/** Matemática pura testável sem SDK: 100% significa um núcleo completamente ocupado. */
object CpuMath {
    fun corePercent(cpuDeltaMs: Long, wallDeltaMs: Long): Double {
        if (wallDeltaMs <= 0 || cpuDeltaMs < 0) return 0.0
        return cpuDeltaMs.toDouble() * 100.0 / wallDeltaMs
    }
}

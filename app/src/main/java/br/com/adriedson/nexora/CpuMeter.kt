package br.com.adriedson.nexora

import android.os.Process
import android.os.SystemClock

/** CPU consumida exclusivamente pelo processo Nexora. NÃO é CPU global do Android. */
class CpuMeter {
    private var previousWallMs = 0L
    private var previousCpuMs = 0L
    data class Reading(val equivalentCorePercent: Double, val allCorePercent: Double, val cores: Int)
    fun sample(): Reading? {
        val wallMs = SystemClock.elapsedRealtime()
        val cpuMs = Process.getElapsedCpuTime()
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val first = previousWallMs == 0L
        val dw = wallMs - previousWallMs
        val dc = cpuMs - previousCpuMs
        previousWallMs = wallMs
        previousCpuMs = cpuMs
        if (first || dw <= 0L || dc < 0L) return null
        val core = CpuMath.corePercent(dc, dw)
        return Reading(core, core / cores, cores)
    }
}


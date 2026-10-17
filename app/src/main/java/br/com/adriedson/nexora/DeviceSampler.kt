package br.com.adriedson.nexora

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.PowerManager

/** Snapshot sob demanda. Temperatura exibida é a da BATERIA, não a do processador. */
object DeviceSampler {
    data class Snapshot(
        val availableMiB: Long,
        val totalMiB: Long,
        val processPssMiB: Long,
        val lowMemory: Boolean,
        val batteryC: Double?,
        val thermalText: String
    )

    fun capture(context: Context): Snapshot {
        val activity = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo().also { activity.getMemoryInfo(it) }
        val process = Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawTemperature = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val temp = rawTemperature.takeIf { it in 1..1000 }?.div(10.0)
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermal = if (Build.VERSION.SDK_INT >= 29) when (power.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> "normal"
            PowerManager.THERMAL_STATUS_LIGHT -> "leve"
            PowerManager.THERMAL_STATUS_MODERATE -> "moderada"
            PowerManager.THERMAL_STATUS_SEVERE -> "severa"
            PowerManager.THERMAL_STATUS_CRITICAL -> "crítica"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "emergência"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "desligamento"
            else -> "indisponível"
        } else "indisponível"
        return Snapshot(
            availableMiB = memory.availMem / 1_048_576L,
            totalMiB = memory.totalMem / 1_048_576L,
            processPssMiB = process.totalPss.toLong() / 1024L,
            lowMemory = memory.lowMemory,
            batteryC = temp,
            thermalText = thermal
        )
    }
}

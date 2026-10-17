package br.com.adriedson.nexora

import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** Telemetria apenas enquanto o pop-up estiver aberto. */
object ResourcePanel {
    fun show(activity: AppCompatActivity) {
        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 24), dp(activity, 12), dp(activity, 24), dp(activity, 12))
        }
        val lines = (0..3).map {
            TextView(activity).apply {
                textSize = 14f
                setTextColor(ThemePreferences.labelColor(activity))
                setPadding(0, dp(activity, 8), 0, dp(activity, 8))
                layout.addView(this, ViewGroup.LayoutParams(-1, -2))
            }
        }
        lines[0].text = "◈ CPU: calculando..."
        lines[1].text = "◈ RAM: calculando..."
        lines[2].text = "◈ Bateria: calculando..."
        lines[3].text = "◈ Estado térmico: calculando..."
        val dlg = MaterialAlertDialogBuilder(activity).setTitle("Recursos do aparelho")
            .setView(layout).setPositiveButton("Fechar", null).create()
        val cpu = CpuMeter()
        var job: Job? = null
        dlg.setOnDismissListener { job?.cancel() }
        dlg.show()
        job = activity.lifecycleScope.launch {
            while (isActive && dlg.isShowing) {
                val snap = withContext(Dispatchers.Default) { runCatching { DeviceSampler.capture(activity.applicationContext) }.getOrNull() }
                val cpuReading = cpu.sample()
                lines[0].text = cpuReading?.let { "◈ CPU Nexora: " +
                    String.format(Locale("pt", "BR"), "%.1f%% (total dos %d núcleos)", it.allCorePercent, it.cores) }
                    ?: "◈ CPU Nexora: amostrando..."
                if (snap != null) {
                    lines[1].text = "◈ RAM: ${snap.availableMiB} / ${snap.totalMiB} MiB disponíveis\n   Nexora: ${snap.processPssMiB} MiB PSS"
                    lines[2].text = "◈ Bateria: ${snap.batteryC?.let { String.format(Locale("pt", "BR"), "%.1f °C", it) } ?: "indisponível"}"
                    lines[3].text = "◈ Pressão térmica: ${snap.thermalText}"
                }
                delay(2500)
            }
        }
    }
    private fun dp(activity: AppCompatActivity, size: Int) = (size * activity.resources.displayMetrics.density + 0.5f).toInt()
}

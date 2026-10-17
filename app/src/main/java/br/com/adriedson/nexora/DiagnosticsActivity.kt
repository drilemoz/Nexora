package br.com.adriedson.nexora

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Leituras a cada 3s SOMENTE com esta tela aberta. Não mantém serviço ou wakelock. */
class DiagnosticsActivity : AppCompatActivity() {
    private lateinit var theme: ThemeOption
    private val cpu = CpuMeter()
    private var lastRecordedAt = 0L
    private lateinit var cpuText: TextView
    private lateinit var cpuGauge: ProgressBar
    private lateinit var memoryText: TextView
    private lateinit var memoryGauge: ProgressBar
    private lateinit var batteryText: TextView
    private lateinit var eventsText: TextView
    private lateinit var countText: TextView

    private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = DiagnosticEvents.report(applicationContext).toByteArray(Charsets.UTF_8)
                    contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                        ?: error("Não foi possível criar arquivo")
                    true
                }.getOrDefault(false)
            }
            Toast.makeText(this@DiagnosticsActivity, if (success) "Relatório JSON exportado" else "Falha ao exportar relatório", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        theme = ThemePreferences.applyBeforeCreate(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostics)
        cpuText = findViewById(R.id.diagCpu)
        cpuGauge = findViewById(R.id.diagCpuProgress)
        memoryText = findViewById(R.id.diagMemory)
        memoryGauge = findViewById(R.id.diagMemoryProgress)
        batteryText = findViewById(R.id.diagThermal)
        eventsText = findViewById(R.id.diagEvents)
        countText = findViewById(R.id.diagCount)
        findViewById<View>(R.id.diagBack).setOnClickListener { finish() }
        findViewById<View>(R.id.diagExport).setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle("Exportar diagnóstico?")
                .setMessage("O JSON contém eventos técnicos, nome do modelo, dados do dispositivo e métricas. Não inclui mensagens ou textos de PDFs. Revise antes de compartilhar.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Escolher destino") { _, _ -> export.launch("Nexora-diagnostico-${System.currentTimeMillis()}.json") }.show()
        }
        findViewById<View>(R.id.diagClear).setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle("Limpar eventos locais?")
                .setMessage("O histórico de diagnósticos será apagado. Conversas e modelos não serão afetados.")
                .setNegativeButton("Cancelar",null).setPositiveButton("Limpar") { _, _ ->
                    DiagnosticEvents.erase(this); refreshEvents()
                }.show()
        }
        refreshEvents()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    val snapshot = withContext(Dispatchers.Default) {
                        runCatching { DeviceSampler.capture(applicationContext) }.getOrNull()
                    }
                    val cpuReading = cpu.sample()
                    if (cpuReading != null) {
                        cpuText.text = String.format(Locale("pt", "BR"), "Nexora: %.1f%% de 1 núcleo  •  %.1f%% da capacidade de %d núcleos",
                            cpuReading.equivalentCorePercent, cpuReading.allCorePercent, cpuReading.cores)
                        cpuGauge.progress = cpuReading.allCorePercent.coerceIn(0.0, 100.0).toInt()
                    } else cpuText.text = "Calculando CPU da Nexora..."
                    if (snapshot != null) {
                        val used = (snapshot.totalMiB - snapshot.availableMiB).coerceAtLeast(0)
                        memoryText.text = "Sistema: $used / ${snapshot.totalMiB} MiB em uso • livres ${snapshot.availableMiB} MiB\n" +
                            "Nexora: ~${snapshot.processPssMiB} MiB (PSS)" + if (snapshot.lowMemory) " • MEMÓRIA BAIXA" else ""
                        memoryGauge.progress = if (snapshot.totalMiB > 0) (used * 100 / snapshot.totalMiB).toInt().coerceIn(0,100) else 0
                        batteryText.text = "Bateria: " + (snapshot.batteryC?.let { "%.1f °C".format(Locale("pt", "BR"), it) } ?: "indisponível") +
                            " • pressão térmica: ${snapshot.thermalText}"
                    }
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (now - lastRecordedAt >= 15_000 && snapshot != null) {
                        lastRecordedAt = now
                        DiagnosticEvents.record(applicationContext, "device.sample",
                            "pssMiB=${snapshot.processPssMiB} ramAvailableMiB=${snapshot.availableMiB} " +
                            "cpuAppPct=${cpuReading?.allCorePercent?.toInt() ?: -1} thermal=${snapshot.thermalText}")
                    }
                    refreshEvents()
                    delay(3000L)
                }
            }
        }
    }

    private fun refreshEvents() {
        val events = DiagnosticEvents.read(this)
        countText.text = "${events.size} eventos recentes • registro local limitado a 256 KiB"
        val format = SimpleDateFormat("dd/MM HH:mm:ss", Locale("pt", "BR"))
        eventsText.text = if(events.isEmpty()) "Nenhum evento registrado." else events.takeLast(65).asReversed().joinToString("\n\n") { e ->
            val stamp = format.format(Date(e.optLong("time")))
            "[$stamp] ${e.optString("level")} • ${e.optString("event")}\n${e.optString("detail")}".trim()
        }
    }
    override fun onResume() {
        super.onResume()
        if (::theme.isInitialized && theme != ThemePreferences.get(this)) recreate()
    }
}

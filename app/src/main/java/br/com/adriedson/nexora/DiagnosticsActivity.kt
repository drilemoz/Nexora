package br.com.adriedson.nexora

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var ramSegments: android.widget.LinearLayout
    private lateinit var diagDetails: android.widget.LinearLayout
    private lateinit var batteryText: TextView
    private lateinit var eventsText: TextView
    private lateinit var countText: TextView

    private var filterMode = 0 // 0=última sessão, 1=versão instalada, 2=todos
    private var showAllEvents = false
    private var exportUri: android.net.Uri? = null

    private fun saveDirect() {
        lifecycleScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { DiagnosticExport.save(applicationContext,DiagnosticEvents.report(applicationContext)) }
            }
            outcome.onSuccess { uri ->
                exportUri = uri
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content),
                    "Salvo em Downloads/Nexora", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                    .setAction("Abrir") {
                        runCatching { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri,"application/json")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }) }.onFailure { Toast.makeText(this@DiagnosticsActivity,"Abra pelo app Arquivos em Downloads/Nexora",Toast.LENGTH_LONG).show() }
                    }.show()
            }.onFailure { Toast.makeText(this@DiagnosticsActivity,"Não foi possível exportar: ${it.javaClass.simpleName}",Toast.LENGTH_LONG).show() }
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
        ramSegments = findViewById(R.id.diagRamSegments)
        diagDetails = findViewById(R.id.diagDetails)
        findViewById<View>(R.id.diagExpand).setOnClickListener {
            val expanded=diagDetails.visibility != View.VISIBLE
            diagDetails.visibility=if(expanded) View.VISIBLE else View.GONE
            findViewById<TextView>(R.id.diagExpand).text=if(expanded) "Recolher detalhes ⌃" else "Ver detalhes ⌄"
        }
        findViewById<View>(R.id.diagShowMore).setOnClickListener {
            showAllEvents = !showAllEvents
            findViewById<TextView>(R.id.diagShowMore).text = if(showAllEvents) "Mostrar menos eventos ⌃" else "Mostrar mais eventos ⌄"
            refreshEvents()
        }
        findViewById<View>(R.id.diagFilter).setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle("Registros exibidos")
                .setSingleChoiceItems(arrayOf("Última sessão", "Versão instalada", "Todos os eventos"),filterMode) { d,which -> filterMode=which;refreshEvents();d.dismiss() }
                .setNegativeButton("Fechar",null).show()
        }
        batteryText = findViewById(R.id.diagThermal)
        eventsText = findViewById(R.id.diagEvents)
        countText = findViewById(R.id.diagCount)
        findViewById<View>(R.id.diagBack).setOnClickListener { finish() }
        findViewById<View>(R.id.diagExport).setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle("Exportar diagnóstico?")
                .setMessage("O JSON contém eventos técnicos, nome do modelo, dados do dispositivo e métricas. Não inclui mensagens ou textos de PDFs. Revise antes de compartilhar.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar em Downloads") { _, _ -> saveDirect() }.show()
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
                        RamUsageBar.render(this@DiagnosticsActivity,ramSegments,snapshot.availableMiB,snapshot.totalMiB)
                        batteryText.text = "Bateria: " + (snapshot.batteryC?.let { "%.1f °C".format(Locale("pt", "BR"), it) } ?: "indisponível") +
                            "\nEstado térmico do Android: ${ThermalDisplay.status(snapshot.thermalText, snapshot.thermalStatusCode)}" +
                            "\nMargem térmica: ${ThermalDisplay.headroom(snapshot.thermalHeadroom)} (não é temperatura da CPU)"
                    }
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (now - lastRecordedAt >= 15_000 && snapshot != null) {
                        lastRecordedAt = now
                        DiagnosticEvents.record(applicationContext, "device.sample",
                            "pssMiB=${snapshot.processPssMiB} ramAvailableMiB=${snapshot.availableMiB} " +
                            "cpuAppPct=${cpuReading?.allCorePercent?.toInt() ?: -1} " +
                                "thermal=${snapshot.thermalText} thermalRaw=${snapshot.thermalStatusCode ?: -1} " +
                                "headroom=${snapshot.thermalHeadroom ?: "NA"}")
                    }
                    refreshEvents()
                    delay(3000L)
                }
            }
        }
    }

    private fun refreshEvents() {
        val all = DiagnosticEvents.read(this)
        val lastStart=all.indexOfLast { it.optString("event")=="app.start" }
        val installedCode=runCatching {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName,0).longVersionCode.toString()
        }.getOrDefault("?")
        var version="?"
        val tagged=all.map { entry ->
            if(entry.optString("event")=="app.start") version=entry.optString("detail").substringAfter("versionCode=","?")
            entry to version
        }
        val events=when(filterMode) {
            0 -> if(lastStart>=0) all.drop(lastStart) else all
            1 -> tagged.filter { it.second==installedCode }.map{it.first}
            else -> all
        }
        countText.text = "${events.size} eventos ${listOf("da sessão","da versão $installedCode","locais")[filterMode]} • limite de 256 KiB"
        val format = SimpleDateFormat("dd/MM HH:mm:ss", Locale("pt", "BR"))
        eventsText.text = if(events.isEmpty()) "Nenhum evento registrado." else events.takeLast(if(showAllEvents)65 else 12).asReversed().joinToString("\n\n") { e ->
            val stamp = format.format(Date(e.optLong("time")))
            "[$stamp] ${e.optString("level")} • ${e.optString("event")}\n${e.optString("detail")}".trim()
        }
    }
    override fun onResume() {
        super.onResume()
        if (::theme.isInitialized && theme != ThemePreferences.get(this)) recreate()
    }
}

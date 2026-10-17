package br.com.adriedson.nexora

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ChatActivity : AppCompatActivity() {
    private lateinit var appliedTheme: ThemeOption
    private lateinit var adapter: ChatAdapter
    private lateinit var store: ChatStore
    private lateinit var list: RecyclerView
    private lateinit var prompt: EditText
    private lateinit var status: TextView
    private lateinit var performanceSpeed: TextView
    private lateinit var performanceGauge: ProgressBar
    private lateinit var performanceGrade: TextView
    private lateinit var performanceLatency: TextView
    private lateinit var performanceMemory: TextView
    private lateinit var performanceThermal: TextView
    private lateinit var performanceNote: TextView
    private val meter = GenerationMeter()
    private var samplingJob: Job? = null
    private lateinit var send: ImageButton
    private lateinit var stop: MaterialButton
    private lateinit var mode: MaterialButton
    private val messages = mutableListOf<ChatMessage>()
    private var busy = false
    private var thinking = false
    private var generation: Job? = null
    private val prefs by lazy { getSharedPreferences("nexora_chat", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedTheme = ThemePreferences.applyBeforeCreate(this)
        super.onCreate(savedInstanceState)
        if (!ModelSession.isReady(this)) {
            Toast.makeText(this, "Carregue um modelo na tela inicial", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setContentView(R.layout.activity_chat)
        store = ChatStore(applicationContext)
        prompt = findViewById(R.id.promptInput)
        status = findViewById(R.id.stateLabel)
        performanceSpeed = findViewById(R.id.performanceSpeed)
        performanceGauge = findViewById(R.id.performanceGauge)
        performanceGrade = findViewById(R.id.performanceGrade)
        performanceLatency = findViewById(R.id.performanceLatency)
        performanceMemory = findViewById(R.id.performanceMemory)
        performanceThermal = findViewById(R.id.performanceThermal)
        performanceNote = findViewById(R.id.performanceNote)
        send = findViewById(R.id.sendMessage)
        stop = findViewById(R.id.stopGeneration)
        mode = findViewById(R.id.thinkMode)
        list = findViewById(R.id.messages)
        findViewById<TextView>(R.id.chatModelName).text = ModelSession.currentName() ?: "Modelo não carregado"
        findViewById<View>(R.id.backChat).setOnClickListener { finish() }
        messages.addAll(store.load())
        adapter = ChatAdapter(messages)
        list.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        list.adapter = adapter
        if (messages.isNotEmpty()) list.scrollToPosition(messages.lastIndex)
        findViewById<View>(R.id.clearChat).setOnClickListener {
            if (!busy) MaterialAlertDialogBuilder(this).setTitle("Apagar conversa?")
                .setMessage("As mensagens salvas nesta Nexora serão excluídas. Os modelos continuarão instalados.")
                .setPositiveButton("Apagar") { _, _ ->
                    messages.clear(); adapter.notifyDataSetChanged(); save()
                }.setNegativeButton("Cancelar", null).show()
        }
        thinking = prefs.getBoolean("thinking_enabled", false)
        mode.setOnClickListener {
            if (busy) return@setOnClickListener
            thinking = !thinking
            prefs.edit().putBoolean("thinking_enabled", thinking).apply()
            updateMode()
            Toast.makeText(this,
                if (thinking) "Qwen3: raciocínio ativado" else "Qwen3: respostas diretas",
                Toast.LENGTH_SHORT).show()
        }
        mode.setOnLongClickListener {
            MaterialAlertDialogBuilder(this).setTitle("Modo de raciocínio")
                .setMessage("Direto envia /no_think. Pensar envia /think para modelos Qwen3. " +
                    "Outros modelos podem não aceitar esses comandos. O texto entre <think> e </think> fica oculto no chat.")
                .setPositiveButton("Entendi", null).show()
            true
        }
        send.setOnClickListener { submit() }
        stop.setOnClickListener { status.text = "Interrompendo geração..."; generation?.cancel() }
        updateMode()
        refresh()
    }

    private fun updateMode() {
        val qwen3 = ModelSession.currentName()?.contains("qwen3", true) == true
        mode.isEnabled = qwen3 && !busy
        mode.text = if (!qwen3) "N/D" else if (thinking) "Pensar" else "Direto"
        mode.contentDescription = if (!qwen3) "Modo de raciocínio disponível para Qwen3" else
            "Modo de raciocínio ${if (thinking) "ativado" else "desativado"}; toque para alternar"
        mode.isCheckable = qwen3
        mode.isChecked = qwen3 && thinking
    }

    private fun submit() {
        if (busy || !ModelSession.isReady(this)) return
        val userText = prompt.text.toString().trim()
        if (userText.isEmpty()) return
        prompt.text.clear()
        messages.add(ChatMessage("user", userText))
        val response = ChatMessage("assistant", "")
        messages.add(response)
        adapter.notifyItemRangeInserted(messages.size - 2, 2)
        list.scrollToPosition(messages.lastIndex)
        meter.start(SystemClock.elapsedRealtimeNanos())
        displayMeter()
        busy = true
        status.text = if (thinking) "Nexora raciocinando no dispositivo..." else "Nexora gerando resposta..."
        refresh()
        val qwen = ModelSession.currentName()?.contains("qwen3", true) == true
        val instruction = if (qwen) "\n${if (thinking) "/think" else "/no_think"}" else ""
        val filter = ThinkingTextFilter()
        generation = lifecycleScope.launch {
            try {
                ModelSession.engine(applicationContext)
                    .sendUserPrompt(userText + instruction, if (thinking) 768 else 512)
                    .collect { token ->
                        meter.onFragment(token, SystemClock.elapsedRealtimeNanos())
                        displayMeter()
                        response.text = filter.append(token)
                        if (response.text.isNotEmpty()) status.text = "Resposta local em andamento..."
                        val index = messages.indexOfLast { it === response }
                        if (index >= 0) { adapter.notifyItemChanged(index); list.scrollToPosition(index) }
                    }
                response.text = filter.finish().ifBlank { "(O modelo não produziu uma resposta final.)" }
                displayMeter()
                status.text = "Resposta concluída • sem conexão com servidores"
            } catch (_: CancellationException) {
                response.text = filter.finish().ifBlank { "(Geração interrompida.)" }
                status.text = "Geração interrompida."
            } catch (e: Exception) {
                response.text = filter.finish().ifBlank { "(Não foi possível gerar a resposta.)" }
                status.text = "Erro de inferência: ${e.localizedMessage?.take(120)}"
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    val i = messages.indexOfLast { it === response }
                    if (i >= 0) adapter.notifyItemChanged(i)
                    busy = false
                    displayMeter()
                    refresh()
                    save()
                }
            }
        }
    }

    private fun save() {
        runCatching { store.save(messages) }
            .onFailure { status.text = "Não foi possível salvar o histórico: ${it.localizedMessage?.take(100)}" }
    }

    private fun refresh() {
        send.isEnabled = !busy && ModelSession.isReady(this)
        prompt.isEnabled = !busy
        stop.visibility = if (busy) View.VISIBLE else View.GONE
        send.visibility = if (busy) View.GONE else View.VISIBLE
        updateMode()
    }

    private fun displayMeter() {
        val reading = meter.reading(SystemClock.elapsedRealtimeNanos())
        performanceSpeed.text = reading.fragmentsPerSecond?.let {
            String.format(Locale("pt", "BR"), "%.1f frag./s ≈ tok/s", it)
        } ?: "— frag./s"
        val rate = reading.fragmentsPerSecond
        performanceGauge.progress = (rate ?: 0.0).coerceIn(0.0, 30.0).toInt()
        performanceGrade.text = when {
            rate == null -> "Aguardando amostras"
            rate < 3.0 -> "Geração lenta"
            rate < 10.0 -> "Geração utilizável"
            rate < 20.0 -> "Geração boa"
            else -> "Geração fluida"
        }
        performanceLatency.text = reading.timeToFirstSeconds?.let {
            String.format(Locale("pt", "BR"), "%.2f s até resposta", it)
        } ?: "Aguardando resposta"
        performanceNote.text = when {
            reading.fragments == 0 -> "A velocidade aparece após os primeiros fragmentos gerados."
            reading.fragments < 2 -> "Recebendo o primeiro fragmento..."
            else -> "${reading.fragments} fragmentos emitidos • estimativa, não contador nativo de tokens"
        }
    }

    private fun sampleDevice() {
        runCatching { DeviceSampler.capture(applicationContext) }.onSuccess { data ->
            performanceMemory.text = "Nexora ~${data.processPssMiB} MiB PSS  •  RAM disponível ${data.availableMiB}/${data.totalMiB} MiB" +
                if (data.lowMemory) "  ⚠ memória baixa" else ""
            val temp = data.batteryC?.let { String.format(Locale("pt", "BR"), "%.1f °C", it) } ?: "indisponível"
            performanceThermal.text = "Bateria $temp  •  pressão térmica ${data.thermalText}"
        }.onFailure {
            performanceMemory.text = "Medição de memória indisponível"
            performanceThermal.text = "Temperatura e pressão térmica indisponíveis"
        }
    }

    override fun onResume() {
        super.onResume()
        sampleDevice()
        samplingJob?.cancel()
        samplingJob = lifecycleScope.launch {
            while (isActive) {
                sampleDevice()
                delay(2000)
            }
        }
        if (::appliedTheme.isInitialized && appliedTheme != ThemePreferences.get(this)) recreate()
    }

    override fun onPause() {
        samplingJob?.cancel()
        samplingJob = null
        super.onPause()
    }

    override fun onStop() {
        generation?.cancel() // sem inferência silenciosa em segundo plano
        super.onStop()
    }
}

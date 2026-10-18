package br.com.adriedson.nexora

import android.content.Context
import android.content.Intent
import com.arm.aichat.NativeGenerationStats
import android.net.Uri
import android.widget.LinearLayout
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.EditText
import android.widget.TextView
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
    private lateinit var chatId: String
    private lateinit var attachmentsRow: LinearLayout
    private lateinit var attachButton: ImageButton
    private var contextReady = false
    private var preparingContext = false
    private var pdfDirty = false
    private var loadedPath: String? = null
    private var firstPrompt = true
    private val pdfPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) attachPdf(uri)
    }
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
    private lateinit var performancePss: TextView
    private lateinit var performanceBattery: TextView
    private lateinit var performanceRamLabel: TextView
    private lateinit var performanceThermalSummary: TextView
    private lateinit var performanceRamBar: ProgressBar
    private lateinit var performanceRamSegments: LinearLayout
    private lateinit var performanceTitleStatus: TextView
    private lateinit var performanceMinimize: TextView
    private lateinit var performanceOverview: LinearLayout
    private var performanceIsMinimized = false
    private lateinit var performanceToggle: TextView
    private lateinit var performanceDetails: LinearLayout
    private lateinit var performanceModel: TextView
    private lateinit var performanceCpu: TextView
    private val cpuMeter = CpuMeter()
    private val meter = GenerationMeter()
    private var lastNativeStats: NativeGenerationStats? = null
    private var peakPssMiB: Long = 0
    private var minRamAvailableMiB: Long = Long.MAX_VALUE
    private var lastInferenceSampleMs: Long = 0
    private var samplingJob: Job? = null
    private lateinit var send: ImageButton
    private lateinit var stop: MaterialButton
    private lateinit var mode: MaterialButton
    private val messages = mutableListOf<ChatMessage>()
    private var busy = false
    private var thinking = false
    private var generation: Job? = null
    private var generationCancelReason = "unknown"
    private val prefs by lazy { getSharedPreferences("nexora_chat", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedTheme = ThemePreferences.applyBeforeCreate(this)
        super.onCreate(savedInstanceState)
        store = ChatStore(applicationContext)
        val existing = intent.getStringExtra(EXTRA_CHAT_ID)?.takeIf { store.exists(it) }
        if (existing == null && !ModelSession.isReady(this)) {
            finish() // não criar conversa fantasma sem modelo
            return
        }
        chatId = existing ?: store.create(ModelSession.currentName())
        setContentView(R.layout.activity_chat)
        prompt = findViewById(R.id.promptInput)
        status = findViewById(R.id.stateLabel)
        performanceSpeed = findViewById(R.id.performanceSpeed)
        performanceGauge = findViewById(R.id.performanceGauge)
        performanceGrade = findViewById(R.id.performanceGrade)
        performanceLatency = findViewById(R.id.performanceLatency)
        performanceMemory = findViewById(R.id.performanceMemory)
        performanceThermal = findViewById(R.id.performanceThermal)
        performanceNote = findViewById(R.id.performanceNote)
        performancePss = findViewById(R.id.performancePss)
        performanceBattery = findViewById(R.id.performanceBattery)
        performanceRamLabel = findViewById(R.id.performanceRamLabel)
        performanceThermalSummary = findViewById(R.id.performanceThermalSummary)
        performanceRamBar = findViewById(R.id.performanceRamBar)
        performanceRamSegments = findViewById(R.id.performanceRamSegments)
        performanceTitleStatus = findViewById(R.id.performanceTitleStatus)
        performanceToggle = findViewById(R.id.performanceToggle)
        performanceMinimize = findViewById(R.id.performanceMinimize)
        performanceOverview = findViewById(R.id.performanceOverview)
        performanceDetails = findViewById(R.id.performanceDetails)
        performanceModel = findViewById(R.id.performanceModel)
        performanceCpu = findViewById(R.id.performanceCpu)
        performanceIsMinimized = prefs.getBoolean("performance_panel_minimized", true)
        updatePerformanceExpansion(prefs.getBoolean("performance_details_expanded", false))
        updatePerformanceVisibility()
        val togglePanel: (View) -> Unit = {
            if (performanceIsMinimized) performanceIsMinimized = false
            else {
                val next = performanceDetails.visibility != View.VISIBLE
                updatePerformanceExpansion(next)
                prefs.edit().putBoolean("performance_details_expanded", next).apply()
            }
            prefs.edit().putBoolean("performance_panel_minimized", performanceIsMinimized).apply()
            updatePerformanceVisibility()
        }
        performanceToggle.setOnClickListener(togglePanel)
        performanceMinimize.setOnClickListener {
            performanceIsMinimized = !performanceIsMinimized
            prefs.edit().putBoolean("performance_panel_minimized", performanceIsMinimized).apply()
            updatePerformanceVisibility()
        }
        findViewById<View>(R.id.performanceHeader).setOnClickListener(togglePanel)
        send = findViewById(R.id.sendMessage)
        stop = findViewById(R.id.stopGeneration)
        mode = findViewById(R.id.thinkMode)
        list = findViewById(R.id.messages)
        attachmentsRow = findViewById(R.id.pdfAttachments)
        attachButton = findViewById(R.id.attachPdf)
        attachButton.setOnClickListener { if (!busy) pdfPicker.launch(arrayOf("application/pdf")) }
        findViewById<TextView>(R.id.chatHeading).text = store.get(chatId)?.title ?: "Conversa"
        findViewById<View>(R.id.chatModelName).setOnClickListener {
            startActivity(Intent(this, ModelsActivity::class.java))
        }
        findViewById<TextView>(R.id.chatModelName).apply {
            text = ModelSession.currentDisplayName(this@ChatActivity) ?: "Modelo não carregado"
            contentDescription = "Modelo em execução: $text. Toque para gerenciar; pressione para ver o nome completo."
            setOnLongClickListener {
                MaterialAlertDialogBuilder(this@ChatActivity).setTitle("Modelo em execução")
                     .setMessage("Nome exibido: ${ModelSession.currentDisplayName(this@ChatActivity) ?: "Nenhum"}\nArquivo GGUF original: ${ModelSession.currentName() ?: "Nenhum"}")
                    .setPositiveButton("Fechar", null).show()
                true
            }
        }
        findViewById<View>(R.id.backChat).setOnClickListener { finish() }
        findViewById<View>(R.id.exportChat).setOnClickListener { requestConversationExport() }
        messages.addAll(store.load(chatId))
        adapter = ChatAdapter(messages) { regenerateLast() }
        list.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        list.adapter = adapter
        if (messages.isNotEmpty()) list.scrollToPosition(messages.lastIndex)
        findViewById<View>(R.id.clearChat).setOnClickListener {
            if (!busy) NexoraDialogs.confirmDelete(this, "Apagar mensagens?",
                "As mensagens desta conversa serão excluídas. Os modelos continuarão instalados.") {
                messages.clear(); adapter.notifyDataSetChanged(); save()
                ModelSession.markContextDirty(chatId)
                contextReady = false
                firstPrompt = true
                pdfDirty = true
                prepareChatContext()
            }
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
        stop.setOnClickListener { generationCancelReason = "user"; status.text = "Interrompendo geração..."; generation?.cancel() }
        updateMode()
        renderPdfs()
        displayMeter()
        refresh()
        prepareChatContext()
        lifecycleScope.launch {
            ModelSession.loadState.collect { loading ->
                when (loading) {
                    is ModelLoadState.Loading -> if(!busy) {
                        status.text="Aguarde: carregando ${loading.name}..."
                        refresh()
                    }
                    is ModelLoadState.Ready -> if(!busy && !preparingContext && !contextReady) {
                        prepareChatContext()
                    }
                    is ModelLoadState.Problem -> if(!busy && !contextReady) status.text=loading.detail
                    else -> Unit
                }
            }
        }
    }
    private fun prepareChatContext() {
        if (preparingContext) return
        preparingContext = true
        contextReady = false
        refresh()
        lifecycleScope.launch {
            try {
                if (ModelSession.isReady(applicationContext)) {
                    status.text = "Preparando contexto desta conversa..."
                    contextReady = ModelSession.prepareChat(applicationContext, chatId)
                    if (contextReady) loadedPath = ModelSession.currentPath()
                }
                if (!contextReady) status.text = "Abra Configurações → Modelos de IA e carregue um GGUF. Seu histórico está salvo."
                else status.text = "Conversa pronta • processamento offline"
            } catch(e:Exception) {
                status.text = "Falha ao preparar modelo: ${e.localizedMessage?.take(95)}"
            } finally { preparingContext = false; refresh() }
        }
    }
    private fun renderPdfs() {
        attachmentsRow.removeAllViews()
        store.pdfs(chatId).forEach { pdf ->
            val btn = com.google.android.material.button.MaterialButton(this).apply {
                text="📄 ${pdf.name.take(23)} • ${pdf.pages} pág.${if(pdf.truncated) " • PARCIAL" else ""}"
                textSize=11f;isAllCaps=false
                setOnClickListener {
                    MaterialAlertDialogBuilder(this@ChatActivity).setTitle(pdf.name)
                        .setMessage("Texto extraído localmente: ${pdf.text.length} caracteres. ${if(pdf.truncated) "Extração parcial." else ""} O modelo recebe somente trechos selecionados (até 1600 caracteres por anexo). Isso não equivale à leitura integral do PDF.")
                        .setPositiveButton("Remover anexo") { _,_ ->store.deletePdf(pdf.id,chatId);pdfDirty=true;renderPdfs()}
                        .setNegativeButton("Fechar",null).show()
                }
            }
            attachmentsRow.addView(btn)
        }
    }
    private fun attachPdf(uri: Uri) {
        if(busy) return
        status.text="Lendo PDF localmente..."
        attachButton.isEnabled=false
        lifecycleScope.launch {
            try {
                val pdf=withContext(Dispatchers.IO) { PdfReader.extract(applicationContext,uri) }
                check(store.pdfs(chatId).size < 5) { "Limite de 5 anexos por conversa" }
                store.addPdf(chatId,pdf.name,pdf.pages,pdf.content,pdf.partial)
                DiagnosticEvents.record(this@ChatActivity, "pdf.attach.ok", "pages=${pdf.pages} chars=${pdf.content.length} truncated=${pdf.partial}")
                pdfDirty = true
                renderPdfs()
                status.text="PDF anexado: ${pdf.name}. IA recebe somente trechos; ${if(pdf.partial) "extração parcial de páginas." else "não leu o documento inteiro."}"
            } catch(e:Exception) {
                DiagnosticEvents.record(this@ChatActivity, "pdf.attach.error", "type=${e.javaClass.simpleName}", "ERROR")
                status.text="PDF não anexado: ${e.localizedMessage?.take(100)}"}
            finally {attachButton.isEnabled=true}
        }
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
        if (busy || !contextReady || !ModelSession.isReady(this)) return
        val userText = prompt.text.toString().trim()
        if (userText.isEmpty()) return
        prompt.text.clear()
        val previous=if(firstPrompt) messages.toList() else emptyList()
        messages.add(ChatMessage("user", userText))
        val response = ChatMessage("assistant", "")
        messages.add(response)
        adapter.notifyItemRangeInserted(messages.size - 2, 2)
        list.scrollToPosition(messages.lastIndex)
        // Operações aritméticas inequívocas são calculadas em decimal localmente.
        // Nunca anunciar uma "calculadora virtual" que o modelo não executou.
        LocalArithmetic.tryCalculate(userText)?.let { calculation ->
            response.text = "Cálculo local verificado:\n" +
                (if (calculation.approximate) "≈ " else "= ") + calculation.value
            adapter.notifyItemChanged(messages.lastIndex)
            status.text = "Cálculo realizado no aparelho • sem inferência de IA"
            DiagnosticEvents.record(this, "math.local.ok", "approximate=${calculation.approximate}")
            save()
            return
        }
        lastNativeStats = null
        peakPssMiB = 0
        minRamAvailableMiB = Long.MAX_VALUE
        lastInferenceSampleMs = 0
        meter.start(SystemClock.elapsedRealtimeNanos())
        DiagnosticEvents.record(this, "inference.start", "model=${ModelSession.currentName()?.take(80) ?: "unknown"} thinking=$thinking")
        displayMeter()
        busy = true
        displayMeter()
        status.text = if (thinking) "Nexora raciocinando no dispositivo..." else "Nexora gerando resposta..."
        refresh()
        val qwen = ModelSession.currentName()?.contains("qwen3", true) == true
        val instruction = if (qwen) "\n${if (thinking) "/think" else "/no_think"}" else ""
        val filter = ThinkingTextFilter()
        val responseIndex = messages.lastIndex
        var lastUiUpdateNs = 0L
        var lastMeterUpdateNs = 0L
        var renderedVisibleChars = 0
        generationCancelReason = "none"
        val configuredTokens = ModelSession.currentPath()?.let { ModelInferencePreferences.get(this, java.io.File(it)).maxTokens } ?: 512
        val generationLimit = if (thinking) (configuredTokens * 3 / 2).coerceAtMost(1536) else configuredTokens
        generation = lifecycleScope.launch {
            try {
                val pdfs = if (firstPrompt || pdfDirty) store.pdfs(chatId) else emptyList()
                val composed = if (firstPrompt || pdfs.isNotEmpty())
                    PdfPrompt.compose(userText, pdfs, if(firstPrompt) previous else emptyList()) else userText
                firstPrompt = false
                pdfDirty = false
                ModelSession.engine(applicationContext)
                    .sendUserPrompt(composed + instruction, generationLimit)
                    .collect { token ->
                        val nowNs = SystemClock.elapsedRealtimeNanos()
                        meter.onFragment(token, nowNs)
                        filter.accept(token)
                        // Não renderizar Markdown dos tokens ocultos nem refazer toda a bolha.
                        val visibleChars = filter.visibleLength()
                        if (StreamingUiPolicy.shouldRenderText(visibleChars, renderedVisibleChars, nowNs, lastUiUpdateNs)) {
                            val visible = filter.visibleText()
                            if (response.text != visible) {
                                val lastVisible = (list.layoutManager as? LinearLayoutManager)
                                    ?.findLastVisibleItemPosition() ?: responseIndex
                                response.text = visible
                                if (visible.isNotEmpty()) status.text = "Resposta local em andamento..."
                                adapter.notifyStreamingTextChanged(responseIndex)
                                if (StreamingUiPolicy.shouldAutoScroll(lastVisible, responseIndex))
                                    list.scrollToPosition(responseIndex)
                            }
                            renderedVisibleChars = visibleChars
                            lastUiUpdateNs = nowNs
                        }
                        // Ainda mostrar contagem/velocidade enquanto o modelo raciocina em silêncio.
                        if (StreamingUiPolicy.shouldUpdateMeter(nowNs, lastMeterUpdateNs)) {
                            displayMeter()
                            lastMeterUpdateNs = nowNs
                        }
                    }
                val engineStats = ModelSession.engine(applicationContext).lastGenerationStats
                lastNativeStats = engineStats
                val visibleFinal = filter.finish()
                response.text = visibleFinal.ifBlank { "(O modelo não produziu texto visível. Pode ter emitido apenas raciocínio oculto.)" }
                if (visibleFinal.isBlank()) DiagnosticEvents.record(this@ChatActivity,"inference.empty_visible",
                    "nativeTokens=${engineStats?.generatedTokens ?: -1} reason=${engineStats?.reason ?: -1}","WARN")
                displayMeter()
                val outcome = GenerationOutcome.fromNative(engineStats?.reason)
                status.text = outcome.status(generationLimit)
                if (outcome != GenerationOutcome.EOG) {
                    ModelSession.markContextDirty(chatId)
                    contextReady = false
                    firstPrompt = true
                    pdfDirty = true
                }
                val reading = meter.reading(SystemClock.elapsedRealtimeNanos())
                DiagnosticEvents.record(this@ChatActivity, "inference.end",
                    "reason=${outcome.diagnostic} tokens=${engineStats?.generatedTokens ?: -1} " +
                        "prompt=${engineStats?.promptTokens ?: -1} ppMs=${engineStats?.promptMs ?: -1} " +
                        "tgMs=${engineStats?.generationMs ?: -1} " +
                        "tps=${engineStats?.tokensPerSecond?.let { String.format(Locale.US, "%.2f", it) } ?: "-1"} " +
                        "pssPeak=$peakPssMiB ramMin=${minRamAvailableMiB.takeIf { it != Long.MAX_VALUE } ?: -1}",
                    if (outcome == GenerationOutcome.DECODE_ERROR) "ERROR" else "INFO")
            } catch (_: CancellationException) {
                response.text = filter.finish().ifBlank { "(Geração interrompida.)" }
                status.text = "Geração interrompida."
                ModelSession.markContextDirty(chatId)
                contextReady = false
                firstPrompt = true
                pdfDirty = true
                DiagnosticEvents.record(this@ChatActivity, "inference.cancel", "reason=$generationCancelReason")
            } catch (e: Exception) {
                response.text = filter.finish().ifBlank { "(Não foi possível gerar a resposta.)" }
                status.text = "Erro de inferência: ${e.localizedMessage?.take(120)}"
                ModelSession.markContextDirty(chatId)
                contextReady = false
                firstPrompt = true
                pdfDirty = true
                DiagnosticEvents.record(this@ChatActivity, "inference.error", "type=${e.javaClass.simpleName}", "ERROR")
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    val i = messages.indexOfLast { it === response }
                    if (i >= 0) adapter.notifyItemChanged(i)
                    busy = false
                    displayMeter()
                    refresh()
                    save()
                    // Restaurar contexto em primeiro plano antes de permitir outra pergunta.
                    if (!contextReady && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED) &&
                        ModelSession.isReady(applicationContext)) prepareChatContext()
                }
            }
        }
    }

    /** Regenera apenas a última resposta. Reinicia o contexto para não herdar a resposta anterior. */
    private fun regenerateLast() {
        if (busy || preparingContext || !ModelSession.isReady(this) || messages.size < 2) return
        val last=messages.lastOrNull() ?: return
        val question=messages.getOrNull(messages.lastIndex-1) ?: return
        if (last.role!="assistant" || question.role!="user") return
        val original=question.text
        busy=true;contextReady=false;refresh()
        status.text="Reiniciando contexto para regenerar a última resposta..."
        lifecycleScope.launch {
            try {
                if(!ModelSession.resetConversation(applicationContext,chatId)) error("Modelo indisponível")
                messages.removeAt(messages.lastIndex)
                messages.removeAt(messages.lastIndex)
                adapter.notifyDataSetChanged()
                prompt.setText(original)
                contextReady=true
                firstPrompt=true
                pdfDirty=true
                busy=false;refresh()
                submit()
            } catch(e:Exception) {
                status.text="Não foi possível regenerar: ${e.localizedMessage?.take(90)}"
                contextReady=ModelSession.isReady(this@ChatActivity)
                busy=false;refresh()
            }
        }
    }

    private fun requestConversationExport() {
        if (busy || preparingContext) {
            Toast.makeText(this, "Finalize a geração antes de exportar.", Toast.LENGTH_SHORT).show()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Exportar conversa e diagnóstico?")
            .setMessage("O JSON incluirá o texto integral das perguntas e respostas, nome do GGUF, " +
                "parâmetros observados e métricas técnicas disponíveis. Não inclui o texto dos PDFs, " +
                "caminhos privados ou credenciais. O arquivo ficará em Downloads/Nexora. " +
                "Compartilhe somente se concordar com a divulgação das mensagens.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Exportar") { _, _ ->
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching {
                        val data = DeviceSampler.capture(applicationContext)
                        val stored = store.load(chatId)
                        val item = store.get(chatId) ?: error("Conversa não encontrada")
                        val report = ConversationExport.build(item, stored, ModelSession.currentName(),
                            ModelSession.cachedTemplateReport(), lastNativeStats,
                            data, thinking, contextReady)
                        ConversationExport.save(applicationContext, item, report)
                    } }
                    result.onSuccess { uri ->
                        DiagnosticEvents.record(this@ChatActivity, "conversation.export", "format=json messages_exported")
                        MaterialAlertDialogBuilder(this@ChatActivity)
                            .setTitle("Conversa exportada")
                            .setMessage("Arquivo salvo em Downloads/Nexora. Você pode compartilhar o JSON para análise, " +
                                "mas ele contém suas perguntas e respostas.")
                            .setNegativeButton("Fechar", null)
                            .setPositiveButton("Compartilhar") { _, _ ->
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    clipData = android.content.ClipData.newUri(contentResolver,
                                        "Conversa e diagnóstico Nexora", uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                startActivity(Intent.createChooser(sendIntent, "Compartilhar diagnóstico da conversa"))
                            }.show()
                    }.onFailure {
                        Toast.makeText(this@ChatActivity, "Falha ao exportar conversa: ${it.javaClass.simpleName}", Toast.LENGTH_LONG).show()
                        DiagnosticEvents.record(this@ChatActivity, "conversation.export.error", "type=${it.javaClass.simpleName}", "ERROR")
                    }
                }
            }.show()
    }

    private fun save() {
        runCatching { store.save(chatId,messages,ModelSession.currentName()) }
            .onFailure { status.text = "Não foi possível salvar o histórico: ${it.localizedMessage?.take(100)}" }
    }

    private fun refresh() {
        send.isEnabled = !busy && contextReady && ModelSession.isReady(this)
        prompt.isEnabled = !busy
        attachButton.isEnabled = !busy
        stop.visibility = if (busy) View.VISIBLE else View.GONE
        send.visibility = if (busy) View.GONE else View.VISIBLE
        updateMode()
    }

    /** Estado mínimo esconde também as métricas resumidas; expansão continua independente. */
    private fun updatePerformanceVisibility() {
        performanceOverview.visibility = if (performanceIsMinimized) View.GONE else View.VISIBLE
        performanceMinimize.text = if (performanceIsMinimized) "□" else "−"
        performanceMinimize.contentDescription = if (performanceIsMinimized)
            "Restaurar painel de desempenho" else "Minimizar painel de desempenho"
        performanceToggle.visibility = if (performanceIsMinimized) View.GONE else View.VISIBLE
    }

    private fun updatePerformanceExpansion(expanded: Boolean) {
        performanceDetails.visibility = if (expanded) View.VISIBLE else View.GONE
        performanceToggle.text = if (expanded) "⌃" else "⌄"
        performanceToggle.contentDescription = if (expanded) "Recolher informações de desempenho" else "Expandir informações de desempenho"
    }

    private fun displayMeter() {
        val reading = meter.reading(SystemClock.elapsedRealtimeNanos())
        performanceSpeed.text = lastNativeStats?.tokensPerSecond?.let {
            String.format(Locale("pt", "BR"), "%.1f tok/s", it)
        } ?: reading.fragmentsPerSecond?.let {
            String.format(Locale("pt", "BR"), "%.1f frag./s", it)
        } ?: "—"
        performanceTitleStatus.text = when {
            busy -> "Gerando"
            lastNativeStats != null -> "Última resposta"
            reading.fragments > 0 -> "Interrompida"
            else -> "Em espera"
        }
        // Nunca simular progresso percentual de resposta; o modelo pode parar antes do limite.
        performanceGauge.visibility = if (busy) View.VISIBLE else View.GONE
        performanceGauge.isIndeterminate = busy
        performanceLatency.text = reading.timeToFirstSeconds?.let {
            String.format(Locale("pt", "BR"), "Primeiro fragmento: %.2f s", it)
        } ?: "Primeiro fragmento: —"
        performanceGrade.text = lastNativeStats?.let { native ->
            "Tokens nativos: ${native.generatedTokens} • geração: ${native.generationMs} ms"
        } ?: "Fragmentos exibidos: ${reading.fragments} • não equivalem necessariamente a tokens"
        performanceNote.text = lastNativeStats?.let { native ->
            "Prompt: ${native.promptTokens} tokens / ${native.promptMs} ms • motivo: ${native.reason}"
        } ?: if (reading.fragments == 0) "Medições de velocidade aparecem após gerar texto." else
            "Velocidade de fragmentos é provisória; tokens/s reais após concluir."
        performanceModel.text = "Modelo: ${ModelSession.currentDisplayName(this) ?: "Nenhum"}\n" +
            "GGUF: ${ModelSession.currentName() ?: "nenhum"}"
    }

    private suspend fun sampleDevice() {
        // Amostragem sob demanda fora da thread de UI. Headroom não é temperatura.
        withContext(Dispatchers.Default) { runCatching { DeviceSampler.capture(applicationContext) } }.onSuccess { data ->
            if (busy) {
                peakPssMiB = maxOf(peakPssMiB, data.processPssMiB)
                minRamAvailableMiB = minOf(minRamAvailableMiB, data.availableMiB)
                val now = SystemClock.elapsedRealtime()
                if (now - lastInferenceSampleMs >= 15_000L) {
                    lastInferenceSampleMs = now
                    DiagnosticEvents.record(this@ChatActivity, "inference.sample",
                        "pssMiB=${data.processPssMiB} availableMiB=${data.availableMiB} " +
                            "thermalRaw=${data.thermalStatusCode ?: -1} " +
                            "headroom=${data.thermalHeadroom ?: "NA"}")
                }
            }
            performancePss.text = "${data.processPssMiB} MiB"
            performanceBattery.text = data.batteryC?.let {
                String.format(Locale("pt", "BR"), "%.1f °C", it)
            } ?: "—"
            performanceRamLabel.text = "RAM disponível: ${data.availableMiB}/${data.totalMiB} MiB"
            performanceRamBar.progress = ThermalDisplay.availableRamProgress(data.availableMiB, data.totalMiB)
            RamUsageBar.render(this@ChatActivity,performanceRamSegments,data.availableMiB,data.totalMiB)
            performanceThermalSummary.text = ThermalDisplay.status(data.thermalText, data.thermalStatusCode)
            if (performanceDetails.visibility == View.VISIBLE) {
                val cpuReading = cpuMeter.sample()
                performanceCpu.text = cpuReading?.let {
                    String.format(Locale("pt", "BR"),
                        "CPU Nexora: %.1f%% da capacidade de %d núcleos", it.allCorePercent, it.cores)
                } ?: "CPU Nexora: aguardando segunda amostra"
            }
            performanceMemory.text = "Nexora: ${data.processPssMiB} MiB PSS • RAM disponível: ${data.availableMiB}/${data.totalMiB} MiB" +
                (if (data.lowMemory) " • Android: memória baixa" else "") +
                (if (peakPssMiB > 0) "\nPico PSS observado durante geração: $peakPssMiB MiB" else "")
            performanceThermal.text = "Estado térmico informado pelo Android: ${ThermalDisplay.status(data.thermalText, data.thermalStatusCode)}" +
                "\nMargem térmica: ${ThermalDisplay.headroom(data.thermalHeadroom)}" +
                "\nBateria: ${performanceBattery.text} (não é temperatura da CPU)"
        }.onFailure {
            performancePss.text = "—"
            performanceBattery.text = "—"
            performanceRamLabel.text = "RAM indisponível"
            performanceRamBar.progress = 0
            performanceRamSegments.removeAllViews()
            performanceThermalSummary.text = "Android: indisponível"
            performanceMemory.text = "Medição de memória indisponível"
            performanceThermal.text = "Estado térmico indisponível"
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::performanceMemory.isInitialized) return
        samplingJob?.cancel()
        samplingJob = lifecycleScope.launch {
            while (isActive) {
                sampleDevice()
                delay(5000)
            }
        }
        if (::appliedTheme.isInitialized && appliedTheme != ThemePreferences.get(this)) recreate()
        else if (::store.isInitialized && !preparingContext && !busy &&
            (loadedPath != ModelSession.currentPath() ||
                (ModelSession.isReady(this) && ModelSession.activeConversationId() != chatId))) {
            contextReady = false
            firstPrompt = true
            findViewById<TextView>(R.id.chatModelName).apply {
            text = ModelSession.currentDisplayName(this@ChatActivity) ?: "Modelo não carregado"
            contentDescription = "Modelo em execução: $text. Toque para gerenciar; pressione para ver o nome completo."
            setOnLongClickListener {
                MaterialAlertDialogBuilder(this@ChatActivity).setTitle("Modelo em execução")
                     .setMessage("Nome exibido: ${ModelSession.currentDisplayName(this@ChatActivity) ?: "Nenhum"}\nArquivo GGUF original: ${ModelSession.currentName() ?: "Nenhum"}")
                    .setPositiveButton("Fechar", null).show()
                true
            }
        }
            prepareChatContext()
        } else if (::store.isInitialized && !contextReady && !preparingContext && ModelSession.isReady(this) && !busy) prepareChatContext()
    }

    override fun onPause() {
        samplingJob?.cancel()
        samplingJob = null
        super.onPause()
    }

    companion object { const val EXTRA_CHAT_ID="chat_id" }
    override fun onStop() {
        if (generation?.isActive == true) generationCancelReason = "app_background"
        generation?.cancel() // sem inferência silenciosa em segundo plano
        super.onStop()
    }
}

package br.com.adriedson.nexora

import android.content.Context
import android.os.SystemClock
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

sealed class ModelLoadState {
    data object Idle : ModelLoadState()
    data class Loading(val name: String, val startedMs: Long, val referenceMs: Long?, val automatic: Boolean) : ModelLoadState()
    data class Ready(val name: String) : ModelLoadState()
    data class Problem(val name: String, val detail: String) : ModelLoadState()
}

/** Executa um modelo por vez e registra o último GGUF carregado com sucesso. */
object ModelSession {
    @Volatile private var path: String? = null
    @Volatile private var activeChat: String? = null
    private val sessionMutex = Mutex()
    private val restoring = AtomicBoolean(false)
    private val scheduled = AtomicBoolean(false)
    private val cancelLoad = AtomicBoolean(false)
    private val worker = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableLoadState = MutableStateFlow<ModelLoadState>(ModelLoadState.Idle)
    val loadState: StateFlow<ModelLoadState> = mutableLoadState.asStateFlow()

    fun currentPath(): String? = path
    fun activeConversationId(): String? = activeChat
    fun currentName(): String? = path?.takeIf { File(it).isFile }?.let { File(it).name }
    fun currentDisplayName(context: Context): String? = path?.takeIf { File(it).isFile }
        ?.let { ModelLabels.display(context, File(it)) }
    fun engine(context: Context): InferenceEngine = AiChat.getInferenceEngine(context.applicationContext)
    fun isReady(context: Context): Boolean = currentName() != null && engine(context).state.value is InferenceEngine.State.ModelReady
    fun isLoading(): Boolean = loadState.value is ModelLoadState.Loading
    fun isCancelRequested(): Boolean = cancelLoad.get()
    fun requestCancelLoad(): Boolean {
        if (!isLoading()) return false
        cancelLoad.set(true)
        return true
    }

    fun startAutomaticRestore(context: Context) {
        val app = context.applicationContext
        if (!ModelStartup.enabled(app) || !ModelStartup.hasRemembered(app)) return
        if (!scheduled.compareAndSet(false, true)) return
        worker.launch {
            try { restoreLastIfEnabled(app) }
            catch (e: Exception) { DiagnosticEvents.record(app, "model.autoload.error", "type=${e.javaClass.simpleName}", "ERROR") }
            finally { scheduled.set(false) }
        }
    }

    fun startManualLoad(context: Context, file: File): Boolean {
        if (!scheduled.compareAndSet(false, true)) return false
        if (isLoading()) { scheduled.set(false); return false }
        val app = context.applicationContext
        cancelLoad.set(false)
        mutableLoadState.value = ModelLoadState.Loading(file.name, SystemClock.elapsedRealtime(),
            ModelStartup.estimatedDuration(app, file), false)
        worker.launch {
            try { load(app, file) }
            catch (_: CancellationException) { /* cancelamento cooperativo registrado abaixo */ }
            catch (error: Exception) {
                if (mutableLoadState.value is ModelLoadState.Loading) {
                    mutableLoadState.value = ModelLoadState.Problem(file.name,
                        "Falha ao carregar: ${error.localizedMessage?.take(120) ?: error.javaClass.simpleName}")
                    DiagnosticEvents.record(app, "model.load.error", "type=${error.javaClass.simpleName}", "ERROR")
                }
            }
            finally { scheduled.set(false) }
        }
        return true
    }

    suspend fun restoreLastIfEnabled(context: Context) {
        val app = context.applicationContext
        if (!ModelStartup.enabled(app) || !ModelStartup.hasRemembered(app)) return
        if (!restoring.compareAndSet(false, true)) return
        try {
            val file = ModelStartup.rememberedFile(app)
            if (file == null) {
                mutableLoadState.value = ModelLoadState.Problem("Modelo anterior", "O último GGUF não foi encontrado. Abra Configurações → Modelos de IA.")
                return
            }
            if (currentPath() == file.absolutePath && isReady(app)) {
                mutableLoadState.value = ModelLoadState.Ready(file.name)
                return
            }
            if (ModelStartup.isPreviousAttemptInterrupted(app, file)) {
                mutableLoadState.value = ModelLoadState.Problem(file.name,
                    "O carregamento anterior foi interrompido. Por segurança, selecione o modelo manualmente em Configurações.")
                return
            }
            cancelLoad.set(false)
            ModelStartup.markAutoAttempt(app, file)
            try { load(app, file, automatic = true) }
            finally { ModelStartup.clearAutoAttempt(app) }
        } finally { restoring.set(false) }
    }

    suspend fun load(context: Context, model: File, automatic: Boolean = false) = withContext(Dispatchers.IO) {
        sessionMutex.withLock {
            require(model.isFile && model.canRead()) { "Modelo não encontrado ou inacessível" }
            if (currentPath() == model.absolutePath && isReady(context)) {
                mutableLoadState.value = ModelLoadState.Ready(model.name)
                return@withLock
            }
            val startMs = SystemClock.elapsedRealtime()
            if (!automatic && !scheduled.get()) cancelLoad.set(false)
            DiagnosticEvents.record(context, "model.load.start", "name=${model.name.take(95)} automatic=$automatic")
            mutableLoadState.value = ModelLoadState.Loading(model.name, startMs,
                ModelStartup.estimatedDuration(context, model), automatic)
            try {
                val e = engine(context)
                withTimeout(20_000) {
                    e.state.first { it is InferenceEngine.State.Initialized ||
                        it is InferenceEngine.State.ModelReady || it is InferenceEngine.State.Error }
                }
                when (e.state.value) {
                    is InferenceEngine.State.ModelReady, is InferenceEngine.State.Error -> e.cleanUp()
                    else -> Unit
                }
                path = null
                activeChat = null
                if (cancelLoad.get()) throw CancellationException("Carregamento cancelado")
                e.loadModel(model.absolutePath)
                if (cancelLoad.get()) {
                    e.cleanUp()
                    throw CancellationException("Carregamento cancelado após liberação do motor")
                }
                path = model.absolutePath
                activeChat = null
                val elapsed = SystemClock.elapsedRealtime() - startMs
                ModelStartup.rememberSuccessful(context, model, elapsed)
                DiagnosticEvents.record(context, "model.load.ready", "name=${model.name.take(95)} elapsedMs=$elapsed")
                mutableLoadState.value = ModelLoadState.Ready(model.name)
            } catch (e: CancellationException) {
                path = null
                activeChat = null
                mutableLoadState.value = ModelLoadState.Idle
                DiagnosticEvents.record(context, "model.load.cancel", "name=${model.name.take(95)}")
                throw e
            } catch (e: Exception) {
                // Uma falha nativa parcial pode deixar buffers alocados. Liberar somente estados seguros.
                try {
                    val failedEngine = engine(context)
                    if (failedEngine.state.value is InferenceEngine.State.Error ||
                        failedEngine.state.value is InferenceEngine.State.ModelReady) failedEngine.cleanUp()
                } catch (_: Exception) { /* preservar falha original no diagnóstico */ }
                DiagnosticEvents.record(context, "model.load.error", "name=${model.name.take(95)} type=${e.javaClass.simpleName}", "ERROR")
                path = null
                activeChat = null
                mutableLoadState.value = ModelLoadState.Problem(model.name,
                    "Falha ao carregar: ${e.localizedMessage?.take(140) ?: e.javaClass.simpleName}")
                throw e
            }
        }
    }

    suspend fun unload(context: Context) = withContext(Dispatchers.IO) {
        sessionMutex.withLock {
            val e = engine(context)
            when (e.state.value) {
                is InferenceEngine.State.ModelReady, is InferenceEngine.State.Error -> e.cleanUp()
                else -> Unit
            }
            path = null
            activeChat = null
            DiagnosticEvents.record(context, "model.unload")
            mutableLoadState.value = ModelLoadState.Idle
        }
    }

    /** Evita reaproveitar tokens de outra conversa quando o modelo permanece no processo. */
    suspend fun prepareChat(context: Context, chatId: String): Boolean = withContext(Dispatchers.IO) {
        sessionMutex.withLock {
            if (!isReady(context)) return@withLock false
            if (activeChat == chatId) return@withLock true
            DiagnosticEvents.record(context, "model.context.switch", "reset=${activeChat != null}")
            if (activeChat == null) { activeChat = chatId; return@withLock true }
            val current = requireNotNull(currentPath())
            val e = engine(context)
            if (e.state.value is InferenceEngine.State.ModelReady) {
                e.cleanUp()
                path = null
                activeChat = null
                try {
                    e.loadModel(current)
                    path = current
                    activeChat = chatId
                    true
                } catch (err: Exception) {
                    mutableLoadState.value = ModelLoadState.Problem(File(current).name, "Falha ao trocar conversa: ${err.localizedMessage}")
                    throw err
                }
            } else false
        }
    }
}

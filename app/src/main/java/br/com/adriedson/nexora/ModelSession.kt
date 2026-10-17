package br.com.adriedson.nexora

import android.content.Context
import android.os.SystemClock
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
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
    private val mutableLoadState = MutableStateFlow<ModelLoadState>(ModelLoadState.Idle)
    val loadState: StateFlow<ModelLoadState> = mutableLoadState.asStateFlow()

    fun currentPath(): String? = path
    fun activeConversationId(): String? = activeChat
    fun currentName(): String? = path?.takeIf { File(it).isFile }?.let { File(it).name }
    fun engine(context: Context): InferenceEngine = AiChat.getInferenceEngine(context.applicationContext)
    fun isReady(context: Context): Boolean = currentName() != null && engine(context).state.value is InferenceEngine.State.ModelReady
    fun isLoading(): Boolean = loadState.value is ModelLoadState.Loading

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
                e.loadModel(model.absolutePath)
                path = model.absolutePath
                activeChat = null
                val elapsed = SystemClock.elapsedRealtime() - startMs
                ModelStartup.rememberSuccessful(context, model, elapsed)
                mutableLoadState.value = ModelLoadState.Ready(model.name)
            } catch (e: Exception) {
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
            mutableLoadState.value = ModelLoadState.Idle
        }
    }

    /** Evita reaproveitar tokens de outra conversa quando o modelo permanece no processo. */
    suspend fun prepareChat(context: Context, chatId: String): Boolean = withContext(Dispatchers.IO) {
        sessionMutex.withLock {
            if (!isReady(context)) return@withLock false
            if (activeChat == chatId) return@withLock true
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

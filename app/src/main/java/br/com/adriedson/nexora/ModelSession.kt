package br.com.adriedson.nexora

import android.content.Context
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

/** Um único modelo por processo. Não persiste uma seleção falsa após o processo morrer. */
object ModelSession {
    @Volatile private var path: String? = null
    @Volatile private var activeChat: String? = null
    fun currentPath(): String? = path
    fun activeConversationId(): String? = activeChat
    fun currentName(): String? = path?.takeIf { File(it).isFile }?.let { File(it).name }
    fun engine(context: Context): InferenceEngine = AiChat.getInferenceEngine(context.applicationContext)
    fun isReady(context: Context): Boolean =
        currentName() != null && engine(context).state.value is InferenceEngine.State.ModelReady

    suspend fun load(context: Context, model: File) = withContext(Dispatchers.IO) {
        require(model.isFile && model.canRead()) { "Modelo não encontrado ou inacessível" }
        val engine = engine(context)
        withTimeout(20_000) {
            engine.state.first { it is InferenceEngine.State.Initialized ||
                it is InferenceEngine.State.ModelReady || it is InferenceEngine.State.Error }
        }
        when (engine.state.value) {
            is InferenceEngine.State.ModelReady, is InferenceEngine.State.Error -> engine.cleanUp()
            else -> Unit
        }
        path = null
        activeChat = null
        engine.loadModel(model.absolutePath)
        path = model.absolutePath
    }

    suspend fun unload(context: Context) = withContext(Dispatchers.IO) {
        val engine = engine(context)
        when (engine.state.value) {
            is InferenceEngine.State.ModelReady, is InferenceEngine.State.Error -> engine.cleanUp()
            else -> Unit
        }
        path = null
        activeChat = null
    }

    /** The JNI engine remembers prior tokens; reset it when opening a different conversation. */
    suspend fun prepareChat(context: Context, chatId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isReady(context)) return@withContext false
        if (activeChat == chatId) return@withContext true
        if (activeChat == null) {
            activeChat = chatId
            return@withContext true
        }
        val current = requireNotNull(currentPath())
        val e = engine(context)
        if (e.state.value is InferenceEngine.State.ModelReady) {
            e.cleanUp()
            path = null
            activeChat = null
            e.loadModel(current)
            path = current
            activeChat = chatId
            true
        } else false
    }
}

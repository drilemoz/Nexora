package br.com.adriedson.nexora

import android.content.Context
import android.net.Uri
import com.arm.aichat.NativeGenerationStats
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Exportação explícita de texto pessoal. Nunca chamada por telemetria automática. */
object ConversationExport {
    fun fileName(time: Long, id: String): String {
        val date = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date(time))
        val safeId = id.filter { it.isLetterOrDigit() }.take(8).ifEmpty { "conversa" }
        return "Nexora-conversa-$date-$safeId.json"
    }

    fun build(
        chat: Conversation,
        history: List<ChatMessage>,
        activeModel: String?,
        template: ModelTemplateReport?,
        native: NativeGenerationStats?,
        device: DeviceSampler.Snapshot,
        thinkModeEnabled: Boolean,
        contextReady: Boolean,
        exportedAt: Long = System.currentTimeMillis()
    ): String {
        val items = JSONArray()
        history.forEachIndexed { index, item ->
            items.put(JSONObject()
                .put("order", index + 1)
                .put("role", item.role)
                .put("content", item.text))
        }
        val lastGeneration = JSONObject()
            .put("scope", "somente_ultima_resposta_desta_sessao_se_disponivel")
            .put("observed", native != null)
        if (native != null) {
            lastGeneration.put("tokensGenerated", native.generatedTokens)
                .put("promptTokens", native.promptTokens)
                .put("promptMs", native.promptMs)
                .put("generationMs", native.generationMs)
                .put("tokensPerSecond", native.tokensPerSecond)
                .put("endReason", native.reason)
        }
        val technical = JSONObject()
            .put("appVersion", "0.1.0-alpha.24")
            .put("modelRecordedInConversation", chat.model)
            .put("modelActiveAtExport", activeModel ?: JSONObject.NULL)
            .put("modelMayHaveChanged", activeModel != null && chat.model != activeModel)
            .put("thinkingModeEnabled", thinkModeEnabled)
            .put("contextReady", contextReady)
            .put("templateFamily", template?.family ?: "nao_verificado")
            .put("templateOrigin", template?.origin ?: "nao_verificado")
            .put("templateApplication", template?.application ?: "nao_verificado")
            .put("templateVerification", template?.verification ?: "nao_verificado")
            .put("llamaCppRevision", template?.engine ?: "nao_verificado")
            .put("memory", JSONObject()
                .put("nexoraPssMiB", device.processPssMiB)
                .put("ramAvailableMiB", device.availableMiB)
                .put("ramTotalMiB", device.totalMiB)
                .put("androidLowMemory", device.lowMemory))
            .put("thermal", JSONObject()
                .put("status", device.thermalText)
                .put("androidStatusCode", device.thermalStatusCode ?: JSONObject.NULL)
                .put("batteryTempC", device.batteryC ?: JSONObject.NULL))
            .put("lastNativeGeneration", lastGeneration)
        return JSONObject()
            .put("format", "nexora-conversation-diagnostic-v1")
            .put("exportedAtEpochMs", exportedAt)
            .put("privacy", "Contem perguntas e respostas completas. Compartilhamento manual apenas.")
            .put("conversation", JSONObject()
                .put("title", chat.title)
                .put("lastUpdatedEpochMs", chat.updated)
                .put("messageCount", history.size)
                .put("messages", items))
            .put("technical", technical)
            .put("limitations", "Métricas são um snapshot na exportação, não medições históricas de cada resposta. " +
                "A exportação não contém o conteúdo de PDFs nem os tokens internos do prompt. " +
                "O arquivo não comprova a causa de eventuais erros matemáticos.")
            .toString(2)
    }

    fun save(context: Context, chat: Conversation, json: String): Uri =
        DiagnosticExport.saveNamed(context, json, fileName(System.currentTimeMillis(), chat.id))
}

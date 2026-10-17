package br.com.adriedson.nexora

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Armazena no sandbox privado. Sem permissões de rede ou backup automático. */
class ChatStore(private val context: Context) {
    private val file get() = File(context.filesDir, "conversation.json")

    fun load(): MutableList<ChatMessage> = try {
        val source = JSONArray(file.takeIf { it.exists() }?.readText() ?: "[]")
        buildList {
            for (i in 0 until source.length()) {
                val item = source.getJSONObject(i)
                val role = item.optString("role")
                val text = item.optString("text")
                if (role in listOf("user", "assistant") && text.isNotBlank()) {
                    add(ChatMessage(role, text))
                }
            }
        }.takeLast(100).toMutableList()
    } catch (_: Exception) { mutableListOf() }

    fun save(messages: List<ChatMessage>) {
        val array = JSONArray()
        messages.takeLast(100).forEach { msg ->
            array.put(JSONObject().put("role", msg.role).put("text", msg.text))
        }
        val target = file
        val temp = File(target.parentFile, "conversation.json.tmp")
        temp.writeText(array.toString())
        if (!temp.renameTo(target)) {
            temp.delete()
            throw IllegalStateException("Não foi possível salvar a conversa")
        }
    }
}

data class ChatMessage(val role: String, var text: String)

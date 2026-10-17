package br.com.adriedson.nexora

import android.content.Context
import java.io.File
import java.security.MessageDigest

/** Preferências de inferência por arquivo GGUF, sem modificar pesos ou nome físico. */
data class ModelInferenceSettings(
    val preset: String = "Equilibrado",
    val contextSize: Int = 4096,
    val threads: Int = 4,
    val temperature: Float = 0.3f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val minP: Float = 0.05f,
    val repeatPenalty: Float = 1.0f,
    val maxTokens: Int = 512
) {
    init {
        require(contextSize in listOf(2048,4096,8192))
        require(threads in 2..4 && temperature in 0f..1.5f && topP in 0.05f..1f)
        require(topK in 1..100 && minP in 0f..1f && repeatPenalty in 1f..1.5f)
        require(maxTokens in 128..1536)
    }
    companion object {
        fun economical() = ModelInferenceSettings("Econômico", 2048,2,0.3f,0.8f,20,0.05f,1.0f,256)
        fun balanced() = ModelInferenceSettings()
        fun customized(context:Int, threads:Int, temp:Float, topP:Float, topK:Int, minP:Float, repeat:Float, tokens:Int) =
            ModelInferenceSettings("Personalizado", context,threads,temp,topP,topK,minP,repeat,tokens)
    }
}

object ModelInferencePreferences {
    private fun key(file: File): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(file.absolutePath.toByteArray(Charsets.UTF_8))
        return bytes.take(12).joinToString("") { "%02x".format(it) }
    }
    private fun prefs(context: Context) = context.getSharedPreferences("nexora_model_inference_v1", Context.MODE_PRIVATE)
    fun get(context: Context, file: File): ModelInferenceSettings {
        val p = prefs(context)
        val k = key(file)
        if (!p.contains("$k.preset")) return ModelInferenceSettings.balanced()
        return runCatching {
            ModelInferenceSettings(p.getString("$k.preset","Equilibrado") ?: "Equilibrado",
                p.getInt("$k.context",4096), p.getInt("$k.threads",4), p.getFloat("$k.temp",0.3f),
                p.getFloat("$k.topP",0.95f),p.getInt("$k.topK",40),p.getFloat("$k.minP",0.05f),
                p.getFloat("$k.repeat",1f),p.getInt("$k.tokens",512))
        }.getOrDefault(ModelInferenceSettings.balanced())
    }
    fun save(context: Context, file: File, s: ModelInferenceSettings) {
        val k=key(file)
        prefs(context).edit().putString("$k.preset",s.preset)
            .putInt("$k.context",s.contextSize).putInt("$k.threads",s.threads).putFloat("$k.temp",s.temperature)
            .putFloat("$k.topP",s.topP).putInt("$k.topK",s.topK).putFloat("$k.minP",s.minP)
            .putFloat("$k.repeat",s.repeatPenalty).putInt("$k.tokens",s.maxTokens).apply()
    }
    fun clear(context: Context, file: File) {
        val k=key(file)
        val editor=prefs(context).edit()
        listOf("preset","context","threads","temp","topP","topK","minP","repeat","tokens").forEach{editor.remove("$k.$it")}
        editor.apply()
    }
}

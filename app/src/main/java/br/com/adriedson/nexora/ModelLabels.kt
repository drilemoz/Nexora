package br.com.adriedson.nexora

import android.content.Context
import java.io.File
import java.security.MessageDigest

/** Apelidos locais; nenhuma renomeação do arquivo GGUF nem mudanças no banco de conversas. */
object ModelLabels {
    private const val PREFS = "nexora_model_labels"
    private fun preferences(context: Context) = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(file.absolutePath.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    fun display(context: Context, file: File): String =
        preferences(context).getString(key(file), null)?.takeIf { it.isNotBlank() }
            ?: ModelDisplayNames.defaultName(file.name)

    fun save(context: Context, file: File, name: String): Boolean {
        val clean = ModelDisplayNames.validCustomName(name) ?: return false
        val editor = preferences(context).edit()
        if (clean == ModelDisplayNames.defaultName(file.name)) editor.remove(key(file))
        else editor.putString(key(file), clean)
        editor.apply()
        return true
    }

    fun clear(context: Context, file: File) {
        preferences(context).edit().remove(key(file)).apply()
    }

    /** Para estados e conversas legados que guardam somente o nome GGUF original. */
    fun fromOriginal(context: Context, original: String): String {
        if (!original.endsWith(".gguf", ignoreCase = true)) return original
        if (runCatching { ModelFiles.safeName(original) }.getOrNull() != original)
            return ModelDisplayNames.defaultName(original)
        val app = context.applicationContext
        val locations = listOfNotNull(
            File(app.filesDir, "models"),
            app.getExternalFilesDir(null)?.let { File(it, "models") }
        )
        val match = locations.asSequence().map { File(it, original) }
            .firstOrNull { it.isFile }
        return if (match != null) display(app, match) else ModelDisplayNames.defaultName(original)
    }
}

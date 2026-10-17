package br.com.adriedson.nexora

import android.content.Context
import java.io.File

/** Persistência conservadora: apenas arquivos GGUF nos diretórios privados do app. */
object ModelStartup {
    private const val PREFS = "nexora_model_startup"
    private const val NAME = "last_name"
    private const val DIR = "last_directory"
    private const val CRASH_MARKER = "pending_autoload"
    private const val ENABLED = "autoload_enabled"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun enabled(context: Context): Boolean = prefs(context).getBoolean(ENABLED, true)
    fun setEnabled(context: Context, enabled: Boolean) { prefs(context).edit().putBoolean(ENABLED, enabled).apply() }
    fun hasRemembered(context: Context): Boolean = prefs(context).contains(NAME)
    private fun modelKey(directory: String, file: File): String = "$directory|${file.name}|${file.length()}|${file.lastModified()}"

    private fun root(context: Context, directory: String): File? = when(directory) {
        "private" -> File(context.filesDir, "models")
        "downloaded" -> context.getExternalFilesDir(null)?.let { File(it, "models") }
        else -> null
    }

    private fun location(context: Context, file: File): String? {
        val canonical = file.canonicalFile
        return listOf("private", "downloaded").firstOrNull { directory ->
            root(context, directory)?.canonicalFile == canonical.parentFile
        }
    }

    fun rememberedFile(context: Context): File? {
        val p = prefs(context)
        val name = p.getString(NAME, null) ?: return null
        if (runCatching { ModelFiles.safeName(name) }.getOrNull() != name) return null
        val base = root(context, p.getString(DIR, null) ?: return null) ?: return null
        return File(base, name).takeIf { it.isFile && it.canRead() && it.length() > 0L }
    }

    fun rememberSuccessful(context: Context, file: File, durationMs: Long) {
        val directory = location(context, file) ?: return
        val p = prefs(context)
        val key = modelKey(directory, file)
        p.edit().putString(NAME, file.name).putString(DIR, directory)
            .putLong("duration:$key", durationMs.coerceAtLeast(1L))
            .remove(CRASH_MARKER).commit()
    }

    fun estimatedDuration(context: Context, file: File): Long? {
        val directory = location(context, file) ?: return null
        return prefs(context).getLong("duration:${modelKey(directory, file)}", 0L).takeIf { it >= 100L }
    }

    fun isPreviousAttemptInterrupted(context: Context, file: File): Boolean {
        val directory = location(context, file) ?: return false
        return prefs(context).getString(CRASH_MARKER, null) == modelKey(directory,file)
    }

    fun markAutoAttempt(context: Context, file: File) {
        val directory = requireNotNull(location(context, file)) { "Arquivo fora da biblioteca privada" }
        check(prefs(context).edit().putString(CRASH_MARKER, modelKey(directory,file)).commit()) { "Não foi possível salvar o estado de carregamento" }
    }
    fun clearAutoAttempt(context: Context) { prefs(context).edit().remove(CRASH_MARKER).commit() }
    fun forgetIfMatches(context: Context, file: File) {
        val p = prefs(context)
        if (p.getString(NAME, null)==file.name && p.getString(DIR, null)==location(context, file)) {
            p.edit().remove(NAME).remove(DIR).remove(CRASH_MARKER).apply()
        }
    }
}

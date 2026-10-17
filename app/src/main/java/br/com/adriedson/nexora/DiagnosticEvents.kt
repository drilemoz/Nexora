package br.com.adriedson.nexora

import android.app.Application
import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Registro local limitado. NÃO armazena prompts, conteúdo PDF, senhas ou caminhos completos. */
object DiagnosticEvents {
    private const val MAX_BYTES = 256 * 1024L
    private const val MAX_LINES = 220
    private const val FILE = "nexora-diagnostics.jsonl"
    private val lock = Any()
    private fun file(context: Context) = File(context.applicationContext.filesDir, FILE)

    /** event é um código controlado pelo app; detail contém apenas estatísticas ou nomes de arquivo sanitizados. */
    fun record(context: Context, event: String, detail: String = "", severity: String = "INFO") {
        runCatching {
            val cleanEvent = event.take(45).replace(Regex("[^a-zA-Z0-9_.-]"), "_")
            val cleanDetail = detail.replace(Regex("[\r\n\t]"), " ").take(140)
            val entry = JSONObject().put("time", System.currentTimeMillis())
                .put("level", severity.take(8)).put("event", cleanEvent).put("detail", cleanDetail)
            synchronized(lock) {
                val target = file(context)
                if (target.length() + entry.toString().length > MAX_BYTES) {
                    val tail = if (target.exists()) target.readLines(Charsets.UTF_8).takeLast(MAX_LINES / 2) else emptyList()
                    val tmp = File(target.parentFile, "$FILE.tmp")
                    tmp.writeText(tail.joinToString("\n", postfix = if (tail.isEmpty()) "" else "\n"), Charsets.UTF_8)
                    if (!tmp.renameTo(target)) { target.delete();tmp.copyTo(target, overwrite=true);tmp.delete() }
                }
                target.appendText(entry.toString() + "\n", Charsets.UTF_8)
            }
        }
    }

    fun read(context: Context): List<JSONObject> = synchronized(lock) {
        runCatching {
            val log = file(context)
            if (!log.exists()) emptyList() else log.readLines(Charsets.UTF_8).takeLast(MAX_LINES)
                .mapNotNull { runCatching { JSONObject(it) }.getOrNull() }
        }.getOrDefault(emptyList())
    }

    fun erase(context: Context) = synchronized(lock) { file(context).delete() }

    fun report(context: Context): String {
        val data = JSONArray()
        read(context).forEach { data.put(it) }
        return JSONObject().put("format", "nexora-diagnostics-v1")
            .put("generatedAt", System.currentTimeMillis())
            .put("app", "Nexora 0.1.0-alpha.11")
            .put("androidSdk", Build.VERSION.SDK_INT)
            .put("device", "${Build.MANUFACTURER.take(40)} ${Build.MODEL.take(60)}")
            .put("processors", Runtime.getRuntime().availableProcessors())
            .put("events", data).toString(2)
    }
}

class NexoraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DiagnosticEvents.record(this, "app.start", "versionCode=11")
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            // Evita exception.message e stack trace completo: podem incluir conteúdo privado.
            DiagnosticEvents.record(this, "app.uncaught", "thread=${thread.name.take(20)} type=${error.javaClass.simpleName}", "ERROR")
            if (previous != null) previous.uncaughtException(thread, error)
            else { android.os.Process.killProcess(android.os.Process.myPid()); kotlin.system.exitProcess(10) }
        }
    }
}

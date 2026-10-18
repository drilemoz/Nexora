package br.com.adriedson.nexora

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Arquivo gerenciado pelo MediaStore em Downloads/Nexora, sem permissões amplas. */
object DiagnosticExport {
    fun displayName(time:Long):String = "Diagnóstico Nexora " + SimpleDateFormat("dd-MM-yyyy HH'h'mm", Locale.forLanguageTag("pt-BR")).format(Date(time))
    fun fileName(time:Long):String = displayName(time) + "-" + SimpleDateFormat("ss-SSS",Locale.ROOT).format(Date(time)) + ".json"
    fun save(context: Context, json:String, time:Long=System.currentTimeMillis()): Uri =
        saveNamed(context, json, fileName(time))

    fun saveNamed(context: Context, json: String, name: String): Uri {
        require(name.matches(Regex("[a-zA-Z0-9._-]{1,125}\\.json"))) { "Nome inválido" }
        require(Build.VERSION.SDK_INT >= 29) { "Exportação direta requer Android 10 ou superior" }
        val values=ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,name)
            put(MediaStore.MediaColumns.MIME_TYPE,"application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/Nexora/")
            put(MediaStore.MediaColumns.IS_PENDING,1)
        }
        val resolver=context.contentResolver
        val uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values) ?: error("Falha ao reservar arquivo em Downloads")
        try {
            resolver.openOutputStream(uri,"w")?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                ?: error("Não foi possível escrever JSON")
            resolver.update(uri,ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING,0) },null,null)
            return uri
        } catch(e:Exception) {
            resolver.delete(uri,null,null)
            throw e
        }
    }
}

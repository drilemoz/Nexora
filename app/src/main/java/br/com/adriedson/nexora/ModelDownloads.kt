package br.com.adriedson.nexora

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/** O DownloadManager transfere mesmo com a tela apagada; nunca baixe modelos via API de inferencia. */
class ModelDownloads(private val context: Context) {
    private val prefs = context.getSharedPreferences("model_download", Context.MODE_PRIVATE)
    private val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    fun dir(): File = File(
        requireNotNull(context.getExternalFilesDir(null)) { "Armazenamento externo privado indisponível" },
        "models"
    ).also { check(it.isDirectory || it.mkdirs()) { "Falha ao criar pasta de modelos" } }
    fun destination(model: CatalogModel): File = File(dir(), model.filename)
    fun partial(model: CatalogModel): File = File(dir(), model.id + ".pending")
    fun activeId(): Long = prefs.getLong("id", -1L)
    fun activeModel(): CatalogModel? = ModelCatalog.byId(prefs.getString("model", null))
    fun isActive(): Boolean = activeId() >= 0 && activeModel() != null
    fun clear() { prefs.edit().remove("id").remove("model").apply() }

    fun enqueue(model: CatalogModel): Long {
        check(!isActive()) { "Há um download em andamento" }
        check(!destination(model).exists()) { "Modelo já baixado" }
        val internal = File(context.filesDir, "models/${model.filename}")
        check(!internal.exists()) { "Modelo já importado" }
        val folder = dir()
        val free = StatFs(folder.absolutePath).availableBytes
        check(free >= model.approximateBytes + 100L * 1024 * 1024) {
            "Espaço insuficiente: reserve ${model.sizeLabel} e mais 100 MiB livres"
        }
        val pending = partial(model)
        if (pending.exists()) check(pending.delete()) { "Não foi possível limpar download anterior" }
        // Downloads HTTPS oficiais, sem entrada de URL arbitraria do usuario.
        val request = DownloadManager.Request(Uri.parse(model.downloadUrl))
            .setTitle("Nexora: ${model.label}")
            .setDescription("Baixando GGUF do Hugging Face")
            .setMimeType("application/octet-stream")
            .setAllowedOverMetered(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, "models/${pending.name}")
        val id = manager.enqueue(request)
        prefs.edit().putLong("id", id).putString("model", model.id).commit()
        return id
    }

    data class Progress(val state: Int, val received: Long, val total: Long, val reason: Int)
    fun progress(): Progress? {
        val id = activeId()
        if (id < 0) return null
        return manager.query(DownloadManager.Query().setFilterById(id))?.use { c ->
            if (!c.moveToFirst()) return@use null
            Progress(
                c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
            )
        }
    }

    fun cancel() {
        val model = activeModel()
        val id = activeId()
        if (id >= 0) manager.remove(id)
        model?.let { partial(it).delete() }
        clear()
    }

    /** Chamado somente apos DownloadManager.STATUS_SUCCESSFUL, fora da UI thread. */
    fun finalizeVerified(model: CatalogModel): File {
        val part = partial(model)
        check(part.isFile) { "Download concluído, mas arquivo nao encontrado" }
        check(part.length() >= model.approximateBytes * 9 / 10) { "Arquivo menor que o esperado" }
        FileInputStream(part).use { stream ->
            val first = ByteArray(4)
            check(stream.read(first) == 4 && ModelFiles.hasGgufMagic(first)) {
                "O arquivo baixado nao é GGUF"
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(part).use { stream ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = stream.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        check(hash.equals(model.sha256, ignoreCase = true)) { "SHA-256 incorreto. Modelo rejeitado." }
        val finalFile = destination(model)
        check(!finalFile.exists()) { "Já existe outro arquivo com este nome" }
        check(part.renameTo(finalFile)) { "Falha ao finalizar o download" }
        return finalFile
    }
}

package br.com.adriedson.nexora

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.StatFs
import android.provider.OpenableColumns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext

class ModelsActivity : AppCompatActivity() {
    private lateinit var activeTheme: ThemeOption
    private lateinit var status: TextView
    private lateinit var detail: TextView
    private lateinit var message: TextView
    private lateinit var chat: MaterialButton
    private lateinit var storageInfo: TextView
    private lateinit var libraryUsage: TextView
    private lateinit var storageUsedBar: android.widget.ProgressBar
    private lateinit var modelList: android.widget.LinearLayout
    private lateinit var importer: MaterialButton
    private lateinit var manage: MaterialButton
    private lateinit var catalog: MaterialButton
    private var busy = false
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importModel(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activeTheme = ThemePreferences.applyBeforeCreate(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_models)
        status = findViewById(R.id.modelStatus)
        detail = findViewById(R.id.deviceStatus)
        message = findViewById(R.id.stateLabel)
        chat = findViewById(R.id.openChat)
        storageInfo = findViewById(R.id.storageInfo)
        libraryUsage = findViewById(R.id.libraryUsage)
        storageUsedBar = findViewById(R.id.storageUsedBar)
        modelList = findViewById(R.id.installedModelList)
        importer = findViewById(R.id.importModel)
        manage = findViewById(R.id.manageModels)
        catalog = findViewById(R.id.openCatalog)
        findViewById<View>(R.id.backFromModels).setOnClickListener {
            if (!busy) finish()
        }
        importer.setOnClickListener { if (!busy) picker.launch(arrayOf("*/*")) }
        manage.setOnClickListener { if (!busy) showModels() }
        catalog.setOnClickListener { if (!busy) startActivity(Intent(this, ModelCatalogActivity::class.java)) }
        chat.setOnClickListener {
            if (ModelSession.isReady(this)) { val id = ChatStore(this).create(ModelSession.currentName()); startActivity(Intent(this, ChatActivity::class.java).putExtra(ChatActivity.EXTRA_CHAT_ID, id)) }
            else toast("Carregue um modelo em Modelos antes de conversar")
        }
        savedInstanceState?.getString(STATE_MESSAGE)?.let { message.text = it }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (::activeTheme.isInitialized && activeTheme != ThemePreferences.get(this)) {
            recreate()
        } else if (::status.isInitialized) refresh()
    }

    private fun modelsDir(): File = File(filesDir, "models").also { check(it.isDirectory || it.mkdirs()) }
    private fun models(): List<File> {
        val local = modelsDir().listFiles().orEmpty().toList()
        val downloaded = runCatching { ModelDownloads(this).dir().listFiles().orEmpty().toList() }
            .getOrDefault(emptyList())
        return (local + downloaded).filter { it.isFile && it.name.endsWith(".gguf", true) }
            .sortedBy { it.name.lowercase() }
    }

    private fun showModels() {
        val all = models()
        if (all.isEmpty()) {
            MaterialAlertDialogBuilder(this).setTitle("Biblioteca vazia")
                .setMessage("Baixe um modelo no catálogo ou importe um arquivo GGUF existente.")
                .setPositiveButton("Explorar") { _, _ -> startActivity(Intent(this, ModelCatalogActivity::class.java)) }
                .setNegativeButton("Fechar", null).show()
            return
        }
        val labels = all.map { "${it.name}\n${it.length() / 1_048_576} MiB" } +
            if (ModelSession.isReady(this)) listOf("Descarregar modelo ativo • liberar RAM") else emptyList()
        MaterialAlertDialogBuilder(this).setTitle("Modelos instalados")
            .setItems(labels.toTypedArray()) { _, index ->
                if (index == all.size) unload() else {
                    val selected = all[index]
                    MaterialAlertDialogBuilder(this).setTitle(selected.name)
                        .setMessage("Carregar para conversar ou remover esta cópia do aparelho.")
                        .setPositiveButton("Carregar") { _, _ -> load(selected) }
                        .setNeutralButton("Excluir") { _, _ -> delete(selected) }
                        .setNegativeButton("Voltar", null).show()
                }
            }.setNegativeButton("Fechar", null).show()
    }

    private fun delete(file: File) {
        if (file.absolutePath == ModelSession.currentPath()) {
            toast("Descarregue o modelo antes de excluir")
            return
        }
        MaterialAlertDialogBuilder(this).setTitle("Excluir ${file.name}?")
            .setMessage("O arquivo privado será apagado. Esta ação não exclui o original de outra pasta.")
            .setPositiveButton("Excluir") { _, _ ->
                toast(if (file.delete()) "Modelo removido" else "Falha ao excluir modelo")
                refresh()
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun load(file: File) {
        if (busy) return
        busy = true
        message.text = "Carregando ${file.name} na memória..."
        refresh()
        lifecycleScope.launch {
            try {
                ModelSession.load(applicationContext, file)
                message.text = "Pronto! Toque em Conversar para começar."
            } catch (e: Exception) {
                message.text = "Falha ao carregar: ${e.localizedMessage?.take(140)}"
                toast("Modelo não carregado")
            } finally {
                busy = false
                refresh()
            }
        }
    }

    private fun unload() {
        if (busy) return
        busy = true
        message.text = "Descarregando modelo..."
        refresh()
        lifecycleScope.launch {
            try {
                ModelSession.unload(applicationContext)
                message.text = "RAM liberada. Selecione outro modelo quando quiser."
            } catch (e: Exception) {
                message.text = "Falha ao descarregar: ${e.localizedMessage?.take(120)}"
            } finally {
                busy = false
                refresh()
            }
        }
    }

    private fun importModel(uri: Uri) {
        if (busy) return
        busy = true
        message.text = "Importando GGUF. Mantenha a Nexora aberta..."
        refresh()
        lifecycleScope.launch {
            try {
                val imported = withContext(Dispatchers.IO) { copyModel(uri) }
                message.text = "${imported.name} importado. Abra Modelos para carregar."
                toast("Modelo importado")
            } catch (_: CancellationException) {
                message.text = "Importação interrompida."
            } catch (e: Exception) {
                message.text = "Falha ao importar: ${e.localizedMessage?.take(120)}"
                toast("Falha na importação")
            } finally {
                busy = false
                refresh()
            }
        }
    }

    private suspend fun copyModel(uri: Uri): File {
        var name = "modelo.gguf"
        var size = -1L
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 && !c.isNull(it) }?.let { name = c.getString(it) }
                c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !c.isNull(it) }?.let { size = c.getLong(it) }
            }
        }
        val safe = ModelFiles.safeName(name)
        val dir = modelsDir()
        if (size > 0) {
            val free = StatFs(dir.absolutePath).availableBytes
            check(size < free && free - size >= 80L * 1024 * 1024) { "Espaço insuficiente para importar" }
        }
        val base = safe.substringBeforeLast('.')
        var target = File(dir, safe)
        var n = 2
        while (target.exists()) target = File(dir, "$base-${n++}.gguf")
        val pending = File.createTempFile("import-", ".partial", dir)
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                val magic = ByteArray(4)
                var readTotal = 0
                while (readTotal < magic.size) {
                    val nread = input.read(magic, readTotal, 4 - readTotal)
                    if (nread < 0) break
                    readTotal += nread
                }
                check(readTotal == 4 && ModelFiles.hasGgufMagic(magic)) { "Formato GGUF inválido" }
                FileOutputStream(pending).use { out ->
                    out.write(magic)
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val nread = input.read(buffer)
                        if (nread < 0) break
                        out.write(buffer, 0, nread)
                    }
                    out.fd.sync()
                }
            } ?: error("O arquivo selecionado não pôde ser aberto")
            check(pending.renameTo(target)) { "Falha ao finalizar importação" }
            return target
        } finally { pending.delete() }
    }

    private fun renderInstalled() {
        modelList.removeAllViews()
        val all = models()
        if (all.isEmpty()) {
            modelList.addView(TextView(this).apply {
                text = "Nenhum GGUF instalado. Importe ou baixe um modelo."
                setTextColor(ThemePreferences.mutedColor(this@ModelsActivity))
                setPadding(12, 12, 12, 12)
            })
        }
        all.forEach { file ->
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(10, 16, 4, 16)
            }
            val label = TextView(this).apply {
                text = "${if (ModelSession.currentPath() == file.absolutePath) "●  " else "◈  "}${file.name}\n${StorageSummary.size(file.length())}"
                setTextColor(ThemePreferences.labelColor(this@ModelsActivity))
                textSize = 13f
                maxLines = 3
            }
            row.addView(label, android.widget.LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(MaterialButton(this).apply {
                text = if (ModelSession.currentPath() == file.absolutePath) "Ativo" else "Carregar"
                textSize = 11f; isAllCaps = false
                isEnabled = !busy && ModelSession.currentPath() != file.absolutePath
                setOnClickListener { load(file) }
            })
            row.addView(android.widget.ImageButton(this).apply {
                setImageResource(R.drawable.ic_delete)
                background = null
                setColorFilter(ThemePreferences.mutedColor(this@ModelsActivity))
                contentDescription = "Excluir ${file.name}"
                setPadding(16,16,16,16)
                setOnClickListener { delete(file) }
            }, android.widget.LinearLayout.LayoutParams(dp(52), dp(52)))
            modelList.addView(row)
            val line = View(this).apply { setBackgroundColor(ThemePreferences.borderColor(this@ModelsActivity)) }
            modelList.addView(line, android.widget.LinearLayout.LayoutParams(-1, dp(1)))
        }
    }
    private fun dp(n: Int) = (n * resources.displayMetrics.density + .5f).toInt()
    private fun refresh() {
        val name = ModelSession.currentName()
        status.text = name ?: "Nenhum modelo carregado"
        val memory = ActivityManager.MemoryInfo()
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
        detail.text = "${if (name == null) "Aguardando modelo" else "● Offline • ARM64"}  ·  ${memory.availMem / 1_048_576} MiB de RAM livres"
        chat.isEnabled = !busy && ModelSession.isReady(this)
        importer.isEnabled = !busy
        manage.isEnabled = !busy
        catalog.isEnabled = !busy
        val used = models().sumOf { it.length() }
        val space = StorageSummary.read(filesDir)
        storageInfo.text = "${StorageSummary.size(space.free)} livres de ${StorageSummary.size(space.total)}"
        libraryUsage.text = "Modelos: ${StorageSummary.size(used)} de ${StorageSummary.size(space.total)} de capacidade  •  ${models().size} instalados"
        storageUsedBar.progress = if (space.total > 0L) ((space.total - space.free).coerceAtLeast(0L) * 100.0 / space.total).toInt().coerceIn(0,100) else 0
        renderInstalled()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_MESSAGE, message.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    companion object { private const val STATE_MESSAGE = "status" }
}

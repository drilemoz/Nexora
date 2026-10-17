package br.com.adriedson.nexora

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class ModelCatalogActivity : AppCompatActivity() {
    private lateinit var theme: ThemeOption
    private lateinit var downloads: ModelDownloads
    private lateinit var cards: LinearLayout
    private lateinit var panel: MaterialCardView
    private lateinit var progress: ProgressBar
    private lateinit var title: TextView
    private lateinit var status: TextView
    private lateinit var cancel: MaterialButton
    private var pollJob: Job? = null
    private var validating: Job? = null
    private var lastActiveId: Long = Long.MIN_VALUE

    override fun onCreate(savedInstanceState: Bundle?) {
        theme = ThemePreferences.applyBeforeCreate(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_catalog)
        downloads = ModelDownloads(applicationContext)
        cards = findViewById(R.id.catalogCards)
        panel = findViewById(R.id.downloadPanel)
        progress = findViewById(R.id.downloadProgress)
        status = findViewById(R.id.downloadStatus)
        title = findViewById(R.id.downloadTitle)
        cancel = findViewById(R.id.cancelDownload)
        findViewById<View>(R.id.backCatalog).setOnClickListener { finish() }
        cancel.setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle("Interromper transferência?")
                .setMessage("O arquivo parcial será apagado. Você poderá iniciar outro download depois.")
                .setPositiveButton("Cancelar download") { _, _ ->
                    if (validating?.isActive == true) {
                        toast("Aguarde o fim da verificação SHA-256")
                    } else {
                        DiagnosticEvents.record(this@ModelCatalogActivity, "model.download.cancel", "id=${downloads.activeModel()?.id ?: "unknown"}")
                        downloads.cancel()
                        renderCards()
                        updateUi()
                    }
                }.setNegativeButton("Continuar", null).show()
        }
        renderCards()
        updateUi()
    }

    override fun onResume() {
        super.onResume()
        if (::theme.isInitialized && theme != ThemePreferences.get(this)) {
            recreate(); return
        }
        pollJob?.cancel()
        pollJob = lifecycleScope.launch {
            while (isActive) {
                updateUi()
                delay(1000)
            }
        }
    }

    override fun onPause() {
        pollJob?.cancel()
        super.onPause()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun renderCards() {
        cards.removeAllViews()
        ModelCatalog.models.forEach { model ->
            val card = MaterialCardView(this).apply {
                radius = dp(22).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                strokeColor = ThemePreferences.borderColor(this@ModelCatalogActivity)
                setCardBackgroundColor(ThemePreferences.panelColor(this@ModelCatalogActivity))
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
            }
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(18), dp(18), dp(18))
            }
            val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val badge = TextView(this).apply {
                text = "✦"
                textSize = 21f
                gravity = Gravity.CENTER
                setTextColor(ThemePreferences.accentColor(this@ModelCatalogActivity))
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
            header.addView(badge, LinearLayout.LayoutParams(dp(34), dp(38)))
            val titleView = TextView(this).apply {
                text = model.label
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ThemePreferences.labelColor(this@ModelCatalogActivity))
                setPadding(dp(6), 0, 0, 0)
            }
            header.addView(titleView, LinearLayout.LayoutParams(0, -2, 1f))
            val description = TextView(this).apply {
                text = "${model.sizeLabel}  ·  ${model.description}"
                textSize = 13f
                setTextColor(ThemePreferences.mutedColor(this@ModelCatalogActivity))
                setLineSpacing(dp(3).toFloat(), 1f)
                setPadding(0, dp(8), 0, dp(14))
            }
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val present = File(filesDir, "models/${model.filename}").isFile ||
                runCatching { downloads.destination(model).isFile }.getOrDefault(false)
            val downloadButton = MaterialButton(this).apply {
                text = if (present) "Instalado" else "Baixar"
                isAllCaps = false
                isEnabled = !present && !downloads.isActive()
                icon = androidx.appcompat.content.res.AppCompatResources.getDrawable(this@ModelCatalogActivity, R.drawable.ic_download)
                iconSize = dp(18)
                iconPadding = dp(6)
                setOnClickListener { confirmDownload(model) }
            }
            val website = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "Página"
                isAllCaps = false
                icon = androidx.appcompat.content.res.AppCompatResources.getDrawable(this@ModelCatalogActivity, R.drawable.ic_external)
                iconSize = dp(18)
                iconPadding = dp(6)
                setOnClickListener { confirmExternal(model) }
            }
            actions.addView(downloadButton, LinearLayout.LayoutParams(0, dp(50), 1f).apply { marginEnd = dp(7) })
            actions.addView(website, LinearLayout.LayoutParams(0, dp(50), 1f).apply { marginStart = dp(7) })
            content.addView(header)
            content.addView(description)
            content.addView(actions)
            card.addView(content)
            cards.addView(card)
        }
    }

    private fun confirmExternal(model: CatalogModel) {
        val url = Uri.parse(model.pageUrl)
        if (url.scheme != "https" || url.host != "huggingface.co") {
            toast("Destino externo inválido"); return
        }
        val content = layoutInflater.inflate(R.layout.dialog_external_link, null)
        content.findViewById<TextView>(R.id.externalDomain).text = "huggingface.co  ·  ${model.label}"
        MaterialAlertDialogBuilder(this)
            .setView(content)
            .setPositiveButton("Abrir página") { _, _ ->
                try { startActivity(Intent(Intent.ACTION_VIEW, url)) }
                catch (_: ActivityNotFoundException) { toast("Nenhum navegador disponível") }
                catch (_: SecurityException) { toast("Não foi possível abrir o link") }
            }
            .setNegativeButton("Permanecer", null)
            .show()
    }

    private fun confirmDownload(model: CatalogModel) {
        if (downloads.isActive()) { toast("Há uma transferência em andamento"); return }
        MaterialAlertDialogBuilder(this)
            .setTitle("Baixar ${model.label}?")
            .setMessage("Arquivo de aproximadamente ${model.sizeLabel}. A transferência usa internet e pode consumir dados móveis. A conversa funciona offline depois de carregar o GGUF." +
                if (model.id == "qwen3-4b") "\n\nATENÇÃO: modelo experimental de 4B. Os 2,5 GB são somente o arquivo; executar exige RAM adicional para contexto e motor. Pode falhar ou fechar em aparelhos com pouca RAM." else "")
            .setPositiveButton("Baixar") { _, _ ->
                try { downloads.enqueue(model); DiagnosticEvents.record(this, "model.download.start", "id=${model.id}"); renderCards(); updateUi() }
                catch (e: Exception) { DiagnosticEvents.record(this, "model.download.error", "id=${model.id} type=${e.javaClass.simpleName}", "ERROR"); toast(e.localizedMessage ?: "Não foi possível iniciar") }
            }
            .setNegativeButton("Agora não", null).show()
    }

    private fun mb(bytes: Long): String = String.format(Locale.ROOT, "%.1f", bytes.coerceAtLeast(0) / 1_000_000.0)

    private fun updateUi() {
        val model = downloads.activeModel()
        val activeId = downloads.activeId()
        if (model == null || activeId < 0L) {
            panel.visibility = View.GONE
            lastActiveId = Long.MIN_VALUE
            return
        }
        if (lastActiveId != activeId) {
            renderCards()
            lastActiveId = activeId
        }
        panel.visibility = View.VISIBLE
        title.text = model.label
        cancel.visibility = if (validating?.isActive == true) View.GONE else View.VISIBLE
        val p = downloads.progress()
        if (p == null) {
            status.text = "Transferência não encontrada. Inicie novamente."
            downloads.cancel()
            renderCards()
            return
        }
        val total = p.total.takeIf { it > 0L }
        val received = p.received.coerceAtLeast(0L)
        val pct = total?.let { (received * 100.0 / it).coerceIn(0.0, 100.0).toInt() }
        progress.isIndeterminate = total == null && p.state != DownloadManager.STATUS_SUCCESSFUL
        if (pct != null) progress.progress = pct
        val bytesText = if (total != null) "${mb(received)} de ${mb(total)} MB" else
            "${mb(received)} MB recebidos • total estimado: ${model.sizeLabel}"
        when (p.state) {
            DownloadManager.STATUS_SUCCESSFUL -> {
                status.text = "Transferido: $bytesText • verificando SHA-256..."
                progress.isIndeterminate = true
                if (validating?.isActive != true) {
                    validating = lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) { downloads.finalizeVerified(model) }
                            DiagnosticEvents.record(this@ModelCatalogActivity, "model.download.verified", "id=${model.id} sizeMb=${model.approximateBytes / 1000000}")
                            toast("Modelo verificado e instalado")
                        } catch (e: Exception) {
                            DiagnosticEvents.record(this@ModelCatalogActivity, "model.download.hash_error", "id=${model.id} type=${e.javaClass.simpleName}", "ERROR")
                            status.text = "Não foi possível validar: ${e.localizedMessage?.take(110)}"
                            toast("Modelo rejeitado. Tente baixar novamente.")
                        } finally {
                            withContext(Dispatchers.IO) { downloads.partial(model).delete() }
                            downloads.clear()
                            lastActiveId = Long.MIN_VALUE
                            renderCards()
                            panel.visibility = View.GONE
                        }
                    }
                }
            }
            DownloadManager.STATUS_FAILED -> {
                DiagnosticEvents.record(this, "model.download.failed", "id=${model.id} reason=${p.reason}", "ERROR")
                status.text = "Falha (código ${p.reason}). $bytesText"
                toast("Download falhou (código ${p.reason}). Verifique a conexão.")
                downloads.cancel()
                renderCards()
                cancel.visibility = View.GONE
            }
            DownloadManager.STATUS_PAUSED -> {
                val why = when (p.reason) {
                    DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Aguardando conexão"
                    DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Aguardando Wi-Fi"
                    DownloadManager.PAUSED_WAITING_TO_RETRY -> "Servidor indisponível; tentando novamente"
                    else -> "Pausado pelo Android"
                }
                status.text = "$why\n$bytesText${pct?.let { "  •  $it%" } ?: ""}"
            }
            DownloadManager.STATUS_RUNNING -> {
                status.text = "Baixando • $bytesText${pct?.let { "  •  $it%" } ?: ""}"
            }
            else -> status.text = "Na fila • $bytesText"
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}

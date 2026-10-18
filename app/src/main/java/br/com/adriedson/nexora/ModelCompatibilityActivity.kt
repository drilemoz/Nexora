package br.com.adriedson.nexora

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** Consulta JNI somente no carregamento e por toque: nenhum monitoramento em segundo plano. */
class ModelCompatibilityActivity : AppCompatActivity() {
    private lateinit var appliedTheme: ThemeOption
    private fun text(id: Int, value: String) { findViewById<TextView>(id).text = value }

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedTheme = ThemePreferences.applyBeforeCreate(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_compatibility)
        findViewById<View>(R.id.backFromCompatibility).setOnClickListener { finish() }
        findViewById<Button>(R.id.verifyTemplateButton).setOnClickListener {
            if (!ModelSession.isReady(this)) return@setOnClickListener
            it.isEnabled = false
            lifecycleScope.launch {
                try {
                    val report = ModelSession.verifyTemplate(this@ModelCompatibilityActivity)
                    render(report)
                    Toast.makeText(this@ModelCompatibilityActivity, "Verificação de formatação concluída", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    text(R.id.templateCompatibility, "Não verificado: falha durante a inspeção do motor.")
                    DiagnosticEvents.record(this@ModelCompatibilityActivity, "template.verify.error", "type=engine", "ERROR")
                } finally { it.isEnabled = ModelSession.isReady(this@ModelCompatibilityActivity) }
            }
        }
        findViewById<Button>(R.id.copyTemplateButton).setOnClickListener {
            val name = ModelSession.currentName()
            val report = ModelSession.cachedTemplateReport()
            if (name == null || report == null) {
                Toast.makeText(this, "Carregue um modelo para copiar o diagnóstico", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Diagnóstico de template Nexora", report.diagnostic(name)))
            Toast.makeText(this, "Diagnóstico copiado sem conteúdo de conversas", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.customTemplateNotice).text =
            "Personalizado (avançado): indisponível neste motor. Não é seguro aceitar templates externos sem validação e isolamento nativo. Cada GGUF usa seus próprios metadados no modo Automático."
    }
    override fun onResume() {
        super.onResume()
        if (appliedTheme != ThemePreferences.get(this)) recreate() else render(ModelSession.cachedTemplateReport())
    }
    private fun render(report: ModelTemplateReport?) {
        val ready = ModelSession.isReady(this)
        val name = ModelSession.currentName()
        text(R.id.templateModel, if (ready) name ?: "Nenhum GGUF" else "Nenhum modelo ativo")
        text(R.id.templateName, if (ready) report?.familyLabel ?: "Não verificado" else "Não identificado")
        text(R.id.templateOrigin, if (ready) report?.originLabel ?: "Não verificado" else "Não verificado")
        text(R.id.templateApplication, if (ready) report?.applicationLabel ?: "Não verificado" else "Não aplicado")
        text(R.id.templateEngine, "llama.cpp ${report?.engine ?: "ea0c4c2c85fb"} (revisão fixa)")
        text(R.id.templateCompatibility, if (ready) report?.compatibilityLabel ?: "Não verificado" else "Carregue um modelo GGUF para testar.")
        text(R.id.templateReasoning, "Raciocínio: não alterado. O teste não gera resposta e não certifica comportamento de <think> em execução.")
        findViewById<Button>(R.id.verifyTemplateButton).isEnabled = ready
        findViewById<Button>(R.id.copyTemplateButton).isEnabled = ready && report != null
    }
}

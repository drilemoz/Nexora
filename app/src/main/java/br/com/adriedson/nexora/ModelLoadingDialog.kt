package br.com.adriedson.nexora

import android.os.SystemClock
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.ProgressBar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog

/** Diálogo de carregamento compartilhado. Não simula progresso nativo indisponível. */
class ModelLoadingDialog(private val activity: AppCompatActivity) {
    private var dialog: AlertDialog? = null
    private var title: TextView? = null
    private var info: TextView? = null
    private var cancel: MaterialButton? = null
    private var progress: ProgressBar? = null

    fun render(state: ModelLoadState) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (state !is ModelLoadState.Loading) { dismiss(); return }
        if (dialog == null) {
            val view = LayoutInflater.from(activity).inflate(R.layout.dialog_model_loading, null)
            title = view.findViewById(R.id.loadingDialogTitle)
            info = view.findViewById(R.id.loadingDialogInfo)
            cancel = view.findViewById(R.id.loadingDialogCancel)
            progress = view.findViewById(R.id.loadingDialogProgress)
            val built = MaterialAlertDialogBuilder(activity).setView(view).setCancelable(false).create()
            built.setCanceledOnTouchOutside(false)
            cancel?.setOnClickListener {
                ModelSession.requestCancelLoad()
                cancel?.isEnabled = false
                cancel?.text = "Cancelamento solicitado"
                info?.text = "Aguardando o motor concluir a etapa atual para liberar memória com segurança."
            }
            dialog = built
            built.show()
        }
        title?.text = "Carregando ${state.name}"
        progress?.isIndeterminate = true // upstream desta revisão não fornece callback nativo ao app
        if (ModelSession.isCancelRequested()) {
            cancel?.isEnabled = false
            cancel?.text = "Cancelamento solicitado"
            info?.text = "Aguardando o motor finalizar a etapa atual antes de liberar memória."
        } else {
            cancel?.isEnabled = true
            cancel?.text = "Cancelar"
            val elapsed = (SystemClock.elapsedRealtime() - state.startedMs).coerceAtLeast(0)
            val estimate = ModelLoadProgress.estimate(elapsed, state.referenceMs)
            info?.text = if (estimate.percentage != null) {
                "${estimate.percentage}% do tempo anterior (estimativa, não progresso real)\n" +
                    "Decorrido ${ModelLoadProgress.clock(elapsed)} • restante estimado ~${ModelLoadProgress.clock(estimate.remainingMs ?: 0)}"
            } else {
                "Decorrido ${ModelLoadProgress.clock(elapsed)}" +
                    if (estimate.exceeded) " • carregamento mais lento que o anterior\nProgresso real indisponível." else
                        "\nProgresso real indisponível nesta versão do motor."
            }
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
        title = null
        info = null
        cancel = null
        progress = null
    }
}

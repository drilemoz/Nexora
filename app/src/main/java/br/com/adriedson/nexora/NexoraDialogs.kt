package br.com.adriedson.nexora

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Componentes padronizados que herdam cores dos temas Nexora. */
object NexoraDialogs {
    fun confirmDelete(activity: AppCompatActivity, title: String, message: String, onDelete: () -> Unit) {
        MaterialAlertDialogBuilder(activity)
            .setIcon(R.drawable.ic_delete)
            .setTitle(title).setMessage(message)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ -> onDelete() }
            .show()
    }

    fun rename(activity: AppCompatActivity, current: String, save: (String) -> Unit) {
        val density = activity.resources.displayMetrics.density
        val input = EditText(activity).apply {
            setText(current); setSelection(text.length); isSingleLine = true
            setTextColor(ThemePreferences.labelColor(activity))
            hint = "Nome da conversa"
        }
        val container = LinearLayout(activity).apply {
            setPadding((24 * density).toInt(), 0, (24 * density).toInt(), 0)
            addView(input, ViewGroup.LayoutParams(-1, -2))
        }
        MaterialAlertDialogBuilder(activity).setTitle("Renomear conversa").setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ -> save(input.text.toString()) }.show()
    }

    fun conversationMenu(activity: AppCompatActivity, pinned: Boolean,
                         onRename: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit) {
        val density = activity.resources.displayMetrics.density
        val sheet = BottomSheetDialog(activity)
        val panel = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((18 * density).toInt(), (14 * density).toInt(),
                (18 * density).toInt(), (26 * density).toInt())
            setBackgroundColor(ThemePreferences.panelColor(activity))
        }
        val options = listOf(Triple("✎  Renomear", onRename, false),
            Triple(if (pinned) "☆  Desafixar" else "☆  Fixar", onPin, false),
            Triple("▢  Excluir", onDelete, true))
        options.forEach { (label, action, destructive) ->
            val row = TextView(activity).apply {
                text = label
                textSize = 16f
                gravity = Gravity.CENTER_VERTICAL
                setPadding((12 * density).toInt(), 0, 0, 0)
                setTextColor(if (destructive) ThemePreferences.accentColor(activity)
                             else ThemePreferences.labelColor(activity))
                setOnClickListener { sheet.dismiss(); action() }
            }
            panel.addView(row, LinearLayout.LayoutParams(-1, (54 * density).toInt()))
        }
        sheet.setContentView(panel)
        sheet.show()
    }
}

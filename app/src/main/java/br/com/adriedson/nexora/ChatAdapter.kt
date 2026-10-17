package br.com.adriedson.nexora

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.drawable.GradientDrawable
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Mensagens do usuário à esquerda e respostas da Nexora à direita. */
class ChatAdapter(private val messages: List<ChatMessage>, private val onRetry: (Int)->Unit = {}) : RecyclerView.Adapter<ChatAdapter.Holder>() {
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val row: LinearLayout = view.findViewById(R.id.messageRow)
        val bubble: LinearLayout = view.findViewById(R.id.messageBubble)
        val role: TextView = view.findViewById(R.id.role)
        val text: TextView = view.findViewById(R.id.content)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
    )
    override fun getItemCount() = messages.size
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val message = messages[position]
        val c = holder.itemView.context
        val user = message.role == "user"
        holder.row.gravity=if (user) Gravity.START else Gravity.END
        val params=holder.bubble.layoutParams as LinearLayout.LayoutParams
        params.width=(holder.itemView.resources.displayMetrics.widthPixels*.84f).toInt().coerceAtLeast(220)
        params.gravity=if (user) Gravity.START else Gravity.END
        holder.bubble.layoutParams=params
        holder.role.text = if (user) "VOCÊ" else "NEXORA"
        holder.role.gravity=if (user) Gravity.START else Gravity.END
        holder.text.text=MessageMarkup.render(message.text)
        val base=ThemePreferences.panelColor(c)
        val accent=ThemePreferences.accentColor(c)
        val bg=if(user) base else androidx.core.graphics.ColorUtils.blendARGB(base,accent,.11f)
        holder.bubble.background=GradientDrawable().apply {
            cornerRadius=dp(c,16).toFloat();setColor(bg)
            setStroke(dp(c,1),ThemePreferences.borderColor(c))
        }
        holder.bubble.setOnLongClickListener {
            val actions=if (!user && position == messages.lastIndex) arrayOf("Copiar texto original","Detalhes","Regenerar última resposta")
                else arrayOf("Copiar texto original","Detalhes")
            MaterialAlertDialogBuilder(c).setTitle(if(user) "Sua mensagem" else "Resposta da Nexora")
                .setItems(actions) { _, which ->
                    if(which==0) {
                        (c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                            .setPrimaryClip(ClipData.newPlainText("Mensagem Nexora",message.text))
                        android.widget.Toast.makeText(c,"Texto copiado",android.widget.Toast.LENGTH_SHORT).show()
                    } else if (which==2) onRetry(position)
                    else MaterialAlertDialogBuilder(c).setTitle("Detalhes da mensagem")
                        .setMessage("Origem: ${if(user) "Você" else "Nexora"}\nCaracteres: ${message.text.length}\nConteúdo armazenado apenas neste aparelho.")
                        .setPositiveButton("Fechar",null).show()
                }.show()
            true
        }
        holder.bubble.contentDescription=(if(user)"Mensagem enviada: " else "Resposta Nexora: ")+message.text
    }
    private fun dp(context: Context,n:Int)=(n*context.resources.displayMetrics.density+.5f).toInt()
}

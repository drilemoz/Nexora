package br.com.adriedson.nexora

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.Holder>() {
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val role: TextView = view.findViewById(R.id.role)
        val text: TextView = view.findViewById(R.id.content)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
    )
    override fun getItemCount() = messages.size
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val message = messages[position]
        holder.role.text = if (message.role == "user") "VOCÊ" else "NEXORA"
        holder.text.text = message.text.ifEmpty { "Gerando..." }
    }
}

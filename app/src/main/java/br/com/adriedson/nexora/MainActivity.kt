package br.com.adriedson.nexora

import android.content.Intent
import android.os.SystemClock
import android.widget.ProgressBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Entrada do app: somente conversas e pesquisa; modelos ficam nas Configurações. */
class MainActivity : AppCompatActivity() {
    private lateinit var theme: ThemeOption
    private lateinit var store: ChatStore
    private lateinit var history: RecyclerView
    private lateinit var search: EditText
    private lateinit var empty: TextView
    private lateinit var loadingCard: View
    private lateinit var loadingTitle: TextView
    private lateinit var loadingInfo: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var loadingDialog: ModelLoadingDialog
    private lateinit var searchRow: View
    override fun onCreate(savedInstanceState: Bundle?) {
        theme=ThemePreferences.applyBeforeCreate(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadingCard=findViewById(R.id.modelLoadingCard)
        loadingTitle=findViewById(R.id.modelLoadingTitle)
        loadingInfo=findViewById(R.id.modelLoadingInfo)
        loadingProgress=findViewById(R.id.modelLoadingProgress)
        loadingDialog=ModelLoadingDialog(this)
        searchRow=findViewById(R.id.searchRow)
        store=ChatStore(this)
        store.migrateLegacy()
        history=findViewById(R.id.historyList)
        empty=findViewById(R.id.historyEmpty)
        search=findViewById(R.id.searchConversations)
        history.layoutManager=LinearLayoutManager(this)
        findViewById<View>(R.id.openResources).setOnClickListener { ResourcePanel.show(this) }
        findViewById<View>(R.id.openDiagnostics).setOnClickListener { startActivity(Intent(this,DiagnosticsActivity::class.java)) }
        findViewById<View>(R.id.openSettings).setOnClickListener { startActivity(Intent(this,SettingsActivity::class.java)) }
        findViewById<View>(R.id.newConversation).setOnClickListener {
            when {
                ModelSession.isLoading() -> loadingDialog.render(ModelSession.loadState.value)
                !ModelSession.isReady(this) -> MaterialAlertDialogBuilder(this)
                    .setTitle("Selecione um modelo")
                    .setMessage("Carregue ou importe um modelo GGUF em Configurações antes de criar uma conversa.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Modelos") { _, _ -> startActivity(Intent(this, ModelsActivity::class.java)) }.show()
                else -> open(store.create(ModelSession.currentName()))
            }
        }
        findViewById<View>(R.id.toggleSearch).setOnClickListener {
            searchRow.visibility=if (searchRow.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (searchRow.visibility == View.VISIBLE) {
                search.requestFocus()
                (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                    .showSoftInput(search, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            } else { search.setText(""); search.clearFocus() }
        }
        search.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start:Int,count:Int,after:Int) = Unit
            override fun onTextChanged(s: CharSequence?,start:Int,before:Int,count:Int) { refresh() }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        refresh()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { ModelSession.loadState.collect { renderLoading() } }
                launch { while (isActive) { renderLoading(); delay(700) } }
            }
        }
        ModelSession.startAutomaticRestore(applicationContext)
    }

    private fun renderLoading() {
        if (!::loadingDialog.isInitialized) return
        val state = ModelSession.loadState.value
        loadingDialog.render(state)
        // Problemas continuam visíveis sem bloquear a interface.
        if (state is ModelLoadState.Problem) {
            loadingCard.visibility = View.VISIBLE
            loadingTitle.text = "Modelo: ${state.name}"
            loadingInfo.text = state.detail
            loadingProgress.isIndeterminate = false
            loadingProgress.progress = 0
        } else loadingCard.visibility = View.GONE
    }
    override fun onDestroy() {
        if (::loadingDialog.isInitialized) loadingDialog.dismiss()
        super.onDestroy()
    }
    override fun onResume() {
        super.onResume()
        if (::theme.isInitialized && theme != ThemePreferences.get(this)) { recreate(); return }
        if (::store.isInitialized) refresh()
    }
    private fun open(id:String) { startActivity(Intent(this,ChatActivity::class.java).putExtra(ChatActivity.EXTRA_CHAT_ID,id)) }
    private fun refresh() {
        val items=store.list(search.text.toString())
        empty.visibility=if(items.isEmpty()) View.VISIBLE else View.GONE
        empty.text=if(search.text.isBlank()) "Sem conversas ainda. Toque em Nova conversa para começar." else "Nenhuma conversa encontrada."
        history.adapter=ConversationAdapter(items,::open,::showActions)
    }
    private fun showActions(view: View, item: Conversation) {
        NexoraDialogs.conversationMenu(this, item.pinned,
            onRename = {
                NexoraDialogs.rename(this, item.title) { newTitle ->
                    runCatching { store.rename(item.id, newTitle) }; refresh()
                }
            },
            onPin = { store.pin(item.id, !item.pinned); refresh() },
            onDelete = {
                NexoraDialogs.confirmDelete(this, "Excluir conversa?",
                    "As mensagens e os textos PDF anexados serão removidos. Os modelos permanecerão instalados.") {
                    store.delete(item.id); refresh()
                }
            }
        )
    }

}

class ConversationAdapter(
    private val items:List<Conversation>,
    private val onOpen:(String)->Unit,
    private val onMenu:(View,Conversation)->Unit
): RecyclerView.Adapter<ConversationAdapter.Holder>() {
    class Holder(v: View):RecyclerView.ViewHolder(v) {
        val title:TextView=v.findViewById(R.id.conversationTitle)
        val model:TextView=v.findViewById(R.id.conversationModel)
        val section:TextView=v.findViewById(R.id.conversationSection)
        val menu:View=v.findViewById(R.id.conversationMenu)
    }
    override fun onCreateViewHolder(p:android.view.ViewGroup,type:Int)=Holder(
        android.view.LayoutInflater.from(p.context).inflate(R.layout.item_conversation,p,false))
    override fun getItemCount()=items.size
    override fun onBindViewHolder(h:Holder,pos:Int) {
        val c=items[pos]
        h.title.text=(if(c.pinned) "★  " else "")+c.title
        h.model.text=c.model
        val day=java.util.Calendar.getInstance().apply {timeInMillis=c.updated}
        val today=java.util.Calendar.getInstance()
        val heading=when {
            c.pinned->"FIXADAS"
            day.get(java.util.Calendar.YEAR)==today.get(java.util.Calendar.YEAR) && day.get(java.util.Calendar.DAY_OF_YEAR)==today.get(java.util.Calendar.DAY_OF_YEAR)->"HOJE"
            day.get(java.util.Calendar.YEAR)==today.get(java.util.Calendar.YEAR) && day.get(java.util.Calendar.DAY_OF_YEAR)==today.get(java.util.Calendar.DAY_OF_YEAR)-1->"ONTEM"
            else->"ANTERIORES"
        }
        val previous=if(pos>0) items[pos-1] else null
        val prevHeading=previous?.let { p ->
            val cal=java.util.Calendar.getInstance().apply {timeInMillis=p.updated}
            when { p.pinned->"FIXADAS";cal.get(java.util.Calendar.YEAR)==today.get(java.util.Calendar.YEAR) && cal.get(java.util.Calendar.DAY_OF_YEAR)==today.get(java.util.Calendar.DAY_OF_YEAR)->"HOJE";
                cal.get(java.util.Calendar.YEAR)==today.get(java.util.Calendar.YEAR) && cal.get(java.util.Calendar.DAY_OF_YEAR)==today.get(java.util.Calendar.DAY_OF_YEAR)-1->"ONTEM";else->"ANTERIORES" }
        }
        h.section.visibility=if(heading!=prevHeading) View.VISIBLE else View.GONE
        h.section.text=heading
        h.itemView.setOnClickListener { onOpen(c.id) }
        h.menu.setOnClickListener { onMenu(it,c) }
    }
}

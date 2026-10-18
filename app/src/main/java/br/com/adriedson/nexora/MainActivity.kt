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
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.Toast
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

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
    private lateinit var activeModelName: TextView
    private lateinit var conversationAdapter: ConversationAdapter
    private var selectionMode = false
    private var deletingHistory = false
    private val selectedIds = mutableSetOf<String>()
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
        activeModelName=findViewById(R.id.activeModelName)
        store=ChatStore(this)
        store.migrateLegacy()
        history=findViewById(R.id.historyList)
        empty=findViewById(R.id.historyEmpty)
        search=findViewById(R.id.searchConversations)
        history.layoutManager=LinearLayoutManager(this)
        conversationAdapter = ConversationAdapter(
            onOpen = ::open, onMenu = ::showActions,
            onToggleSelection = ::toggleSelection, onSelectionMode = ::toggleSelectionMode,
            onSelectAll = ::toggleAllSelection, onBulkDelete = ::confirmBulkDelete
        )
        history.adapter = conversationAdapter
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
            override fun onTextChanged(s: CharSequence?,start:Int,before:Int,count:Int) {
                selectionMode = false
                selectedIds.clear()
                refresh()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        refresh()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { ModelSession.loadState.collect { renderLoading() } }
                launch { while (isActive) { renderLoading(); delay(700) } }
                // Reagrupa conversas em "Hoje" / data ao passar da meia-noite,
                // inclusive quando a tela permanece aberta.
                launch { while (isActive) {
                    val now = Calendar.getInstance()
                    val next = (now.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    delay((next.timeInMillis - now.timeInMillis).coerceAtLeast(1_000L))
                    refresh()
                } }
            }
        }
        ModelSession.startAutomaticRestore(applicationContext)
    }

    private fun renderLoading() {
        if (!::loadingDialog.isInitialized) return
        val state = ModelSession.loadState.value
        loadingDialog.render(state)
        val current = ModelSession.currentDisplayName(this)
        activeModelName.text = when {
            state is ModelLoadState.Loading -> "Carregando: ${ModelLabels.fromOriginal(this, state.name)}"
            current != null -> "Modelo: $current"
            else -> "Nenhum modelo carregado"
        }
        activeModelName.contentDescription = activeModelName.text
        // Problemas continuam visíveis sem bloquear a interface.
        if (state is ModelLoadState.Problem) {
            loadingCard.visibility = View.VISIBLE
            loadingTitle.text = "Modelo: ${ModelLabels.fromOriginal(this, state.name)}"
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
        selectedIds.retainAll(items.mapTo(HashSet()) { it.id })
        if (items.isEmpty()) { selectionMode = false; selectedIds.clear() }
        conversationAdapter.replace(items, selectionMode, selectedIds)
    }
    private fun toggleSelectionMode() {
        if (deletingHistory) return
        selectionMode = !selectionMode
        selectedIds.clear()
        refresh()
    }
    private fun toggleSelection(id: String) {
        if (deletingHistory) return
        selectionMode = true
        if (!selectedIds.add(id)) selectedIds.remove(id)
        refresh()
    }
    private fun toggleAllSelection() {
        if (deletingHistory) return
        val visibleIds = store.list(search.text.toString()).mapTo(HashSet()) { it.id }
        if (selectedIds.containsAll(visibleIds)) selectedIds.clear() else selectedIds.addAll(visibleIds)
        selectionMode = true
        refresh()
    }
    private fun confirmBulkDelete() {
        if (deletingHistory) return
        val ids = selectedIds.toSet()
        if (selectionMode && ids.isEmpty()) {
            Toast.makeText(this, "Selecione pelo menos uma conversa.", Toast.LENGTH_SHORT).show()
            return
        }
        val total = if (selectionMode) ids.size else store.count()
        if (total == 0) return
        val all = !selectionMode
        val message = if (all) "Todas as $total conversas serão excluídas, incluindo fixadas."
            else "$total conversas selecionadas serão excluídas."
        NexoraDialogs.confirmDelete(this,
            if (all) "Excluir todas as conversas?" else "Excluir $total conversas?",
            "$message\n\nMensagens e textos PDF anexados também serão apagados permanentemente. Os modelos GGUF permanecerão instalados."
        ) {
            deletingHistory = true
            lifecycleScope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        if (all) store.deleteAll() else store.deleteMany(ids)
                    }
                }
                deletingHistory = false
                result.onSuccess { count ->
                    selectedIds.clear(); selectionMode = false
                    refresh()
                    Toast.makeText(this@MainActivity, "$count conversas excluídas", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(this@MainActivity, "Não foi possível excluir as conversas.", Toast.LENGTH_LONG).show()
                    refresh()
                }
            }
        }
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
    private val onOpen: (String) -> Unit,
    private val onMenu: (View, Conversation) -> Unit,
    private val onToggleSelection: (String) -> Unit,
    private val onSelectionMode: () -> Unit,
    private val onSelectAll: () -> Unit,
    private val onBulkDelete: () -> Unit
) : RecyclerView.Adapter<ConversationAdapter.Holder>() {
    private var items: List<Conversation> = emptyList()
    private var selecting: Boolean = false
    private var selected: Set<String> = emptySet()

    fun replace(conversations: List<Conversation>, selectionMode: Boolean, selectedIds: Set<String>) {
        items = conversations
        selecting = selectionMode
        selected = selectedIds.toSet()
        notifyDataSetChanged()
    }

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.conversationTitle)
        val model: TextView = v.findViewById(R.id.conversationModel)
        val section: TextView = v.findViewById(R.id.conversationSection)
        val sectionRow: View = v.findViewById(R.id.conversationSectionRow)
        val selectMode: com.google.android.material.button.MaterialButton = v.findViewById(R.id.historySelectMode)
        val selectAll: com.google.android.material.button.MaterialButton = v.findViewById(R.id.historySelectAll)
        val deleteHistory: ImageButton = v.findViewById(R.id.historyBulkDelete)
        val check: CheckBox = v.findViewById(R.id.conversationSelected)
        val icon: View = v.findViewById(R.id.conversationIcon)
        val menu: View = v.findViewById(R.id.conversationMenu)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, type: Int) = Holder(
        android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
    )
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val heading = ConversationDateGroups.section(item.updated, item.pinned, today, zone)
        val previous = items.getOrNull(position - 1)
        val previousHeading = previous?.let { ConversationDateGroups.section(it.updated, it.pinned, today, zone) }
        val isFirst = position == 0
        holder.sectionRow.visibility = if (heading != previousHeading) View.VISIBLE else View.GONE
        holder.section.text = if (isFirst && selecting && selected.isNotEmpty())
            "$heading · ${selected.size}" else heading
        holder.selectMode.visibility = if (isFirst) View.VISIBLE else View.GONE
        holder.selectAll.visibility = if (isFirst && selecting) View.VISIBLE else View.GONE
        holder.deleteHistory.visibility = if (isFirst) View.VISIBLE else View.GONE
        holder.selectMode.text = if (selecting) "Cancelar" else "Selecionar"
        holder.selectMode.contentDescription = if (selecting) "Cancelar seleção" else "Selecionar várias conversas"
        holder.selectAll.text = if (selected.size == items.size) "Nenhuma" else "Todas"
        holder.selectAll.contentDescription = if (selected.size == items.size)
            "Desmarcar todas as conversas exibidas" else "Selecionar todas as conversas exibidas"
        holder.deleteHistory.contentDescription = if (selecting)
            "Excluir ${selected.size} conversas selecionadas" else "Excluir todas as conversas"
        holder.selectMode.setOnClickListener { onSelectionMode() }
        holder.selectAll.setOnClickListener { onSelectAll() }
        holder.deleteHistory.setOnClickListener { onBulkDelete() }

        holder.title.text = (if (item.pinned) "★  " else "") + item.title
        val name = ModelLabels.fromOriginal(holder.itemView.context, item.model)
        holder.model.text = if (item.pinned) "$name · ${ConversationDateGroups.pinnedDetail(item.updated, zone)}" else name
        holder.check.visibility = if (selecting) View.VISIBLE else View.GONE
        holder.icon.visibility = if (selecting) View.GONE else View.VISIBLE
        holder.check.isChecked = item.id in selected
        holder.menu.visibility = if (selecting) View.GONE else View.VISIBLE
        holder.itemView.isActivated = item.id in selected
        holder.itemView.setOnClickListener {
            if (selecting) onToggleSelection(item.id) else onOpen(item.id)
        }
        holder.itemView.setOnLongClickListener {
            onToggleSelection(item.id)
            true
        }
        holder.menu.setOnClickListener { onMenu(it, item) }
    }
}

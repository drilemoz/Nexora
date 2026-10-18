package br.com.adriedson.nexora

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import java.io.File
import java.util.UUID

data class ChatMessage(val role: String, var text: String)
data class Conversation(val id: String, val title: String, val model: String, val updated: Long, val pinned: Boolean)
data class PdfAttachment(val id: String, val name: String, val pages: Int, val text: String, val truncated: Boolean)

/** SQLite local, sem sincronização externa, banco de conversas isoladas. */
class ChatStore(context: Context) : SQLiteOpenHelper(context.applicationContext, "nexora_history.db", null, 1) {
    private val app = context.applicationContext
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE conversations (id TEXT PRIMARY KEY, title TEXT NOT NULL, model TEXT NOT NULL, updated INTEGER NOT NULL, pinned INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, conversation_id TEXT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE, role TEXT NOT NULL, content TEXT NOT NULL)")
        db.execSQL("CREATE INDEX msg_chat ON messages(conversation_id,id)")
        db.execSQL("CREATE TABLE pdfs (id TEXT PRIMARY KEY, conversation_id TEXT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE, name TEXT NOT NULL, pages INTEGER NOT NULL, content TEXT NOT NULL, truncated INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX pdf_chat ON pdfs(conversation_id)")
    }
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { error("Migração de banco não definida: $oldVersion → $newVersion") }
    private fun values(vararg items: Pair<String, Any>): ContentValues = ContentValues().apply {
        for ((key,value) in items) when (value) {
            is String -> put(key,value)
            is Long -> put(key,value)
            is Int -> put(key,value)
        }
    }
    fun create(model: String? = null): String {
        val id = UUID.randomUUID().toString()
        writableDatabase.insertOrThrow("conversations", null, values("id" to id,"title" to "Nova conversa", "model" to (model ?: "Sem modelo"), "updated" to System.currentTimeMillis(),"pinned" to 0))
        return id
    }
    fun exists(id: String): Boolean = readableDatabase.rawQuery("SELECT 1 FROM conversations WHERE id=?",arrayOf(id)).use { it.moveToFirst() }
    fun get(id: String): Conversation? = readableDatabase.rawQuery("SELECT id,title,model,updated,pinned FROM conversations WHERE id=?",arrayOf(id)).use { c ->
        if (c.moveToFirst()) Conversation(c.getString(0),c.getString(1),c.getString(2),c.getLong(3),c.getInt(4)==1) else null
    }
    fun list(query: String = ""): List<Conversation> {
        val q = query.trim().take(100)
        val filter = if (q.isBlank()) "" else " WHERE title LIKE ? OR EXISTS (SELECT 1 FROM messages m WHERE m.conversation_id=conversations.id AND m.content LIKE ?)"
        val pattern = "%" + q + "%"
        val args = if (q.isBlank()) emptyArray() else arrayOf(pattern,pattern)
        return readableDatabase.rawQuery("SELECT id,title,model,updated,pinned FROM conversations$filter ORDER BY pinned DESC,updated DESC",args).use { c ->
            buildList { while (c.moveToNext()) add(Conversation(c.getString(0),c.getString(1),c.getString(2),c.getLong(3),c.getInt(4)==1)) }
        }
    }
    fun rename(id: String, title: String) {
        val safe=title.trim().take(90)
        require(safe.isNotBlank())
        writableDatabase.update("conversations", values("title" to safe),"id=?",arrayOf(id))
    }
    fun pin(id: String, pinned: Boolean) { writableDatabase.update("conversations",values("pinned" to if(pinned)1 else 0),"id=?",arrayOf(id)) }
    fun delete(id: String) { writableDatabase.delete("conversations","id=?",arrayOf(id)) }
    fun load(id: String): MutableList<ChatMessage> = readableDatabase.rawQuery("SELECT role,content FROM messages WHERE conversation_id=? ORDER BY id",arrayOf(id)).use { c ->
        buildList { while(c.moveToNext()) add(ChatMessage(c.getString(0),c.getString(1))) }.toMutableList()
    }
    fun save(id: String, messages: List<ChatMessage>, model: String?) {
        require(exists(id)) { "Conversa inexistente" }
        val db=writableDatabase
        // Mantém somente as 200 mensagens mais recentes e os mesmos limites anteriores.
        val desired = messages.takeLast(200)
            .filter { (it.role == "user" || it.role == "assistant") && it.text.isNotBlank() }
            .map { ChatMessage(it.role, it.text.take(32000)) }
        db.beginTransaction()
        try {
            // Localiza o maior sufixo antigo igual ao prefixo desejado.
            // Ex.: [A,B,C] -> [A,B,C,D] insere apenas D; após ultrapassar 200,
            // remove somente as linhas mais antigas, preservando IDs recentes.
            val diskIds = ArrayList<Long>()
            val diskMessages = ArrayList<ChatMessage>()
            db.rawQuery("SELECT id,role,content FROM messages WHERE conversation_id=? ORDER BY id",arrayOf(id)).use { c ->
                while(c.moveToNext()) {
                    diskIds.add(c.getLong(0))
                    diskMessages.add(ChatMessage(c.getString(1), c.getString(2)))
                }
            }
            val kept = MessageSaveDiff.retainedCount(diskMessages, desired)
            val removed = diskIds.size - kept
            if (removed > 0) db.delete("messages", "conversation_id=? AND id<=?",
                arrayOf(id, diskIds[removed - 1].toString()))
            for (item in desired.drop(kept)) {
                db.insertOrThrow("messages", null,
                    values("conversation_id" to id, "role" to item.role, "content" to item.text))
            }
            val lastQuestion = messages.firstOrNull { it.role=="user" }?.text?.lineSequence()?.firstOrNull()?.take(65)?.trim()
            val old=get(id)
            val newTitle = if(old?.title == "Nova conversa" && !lastQuestion.isNullOrBlank()) lastQuestion else old?.title ?: "Nova conversa"
            db.update("conversations",values("title" to newTitle,"updated" to System.currentTimeMillis(), "model" to (model ?: "Sem modelo")),"id=?",arrayOf(id))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    fun addPdf(chatId: String, name: String, pages: Int, text: String, truncated: Boolean): PdfAttachment {
        require(exists(chatId))
        val item=PdfAttachment(UUID.randomUUID().toString(),name.take(130),pages,text.take(24000),truncated)
        writableDatabase.insertOrThrow("pdfs",null, values("id" to item.id, "conversation_id" to chatId, "name" to item.name,"pages" to item.pages,"content" to item.text,"truncated" to if(item.truncated)1 else 0))
        return item
    }
    fun pdfs(chatId: String): List<PdfAttachment> = readableDatabase.rawQuery("SELECT id,name,pages,content,truncated FROM pdfs WHERE conversation_id=? ORDER BY rowid DESC",arrayOf(chatId)).use { c ->
        buildList { while(c.moveToNext()) add(PdfAttachment(c.getString(0),c.getString(1),c.getInt(2),c.getString(3),c.getInt(4)!=0)) }
    }
    fun deletePdf(id: String, chatId: String) { writableDatabase.delete("pdfs","id=? AND conversation_id=?",arrayOf(id,chatId)) }
    /** Old alpha.7 conversation.json is copied once, never silently discarded. */
    fun migrateLegacy() {
        val legacy=File(app.filesDir,"conversation.json")
        if(!legacy.isFile) return
        val items=runCatching {
            val array=JSONArray(legacy.readText())
            buildList { for(i in 0 until array.length()) {
                val obj=array.getJSONObject(i)
                val role=obj.optString("role");val text=obj.optString("text")
                if(role=="user" || role=="assistant") if(text.isNotBlank()) add(ChatMessage(role,text))
            } }
        }.getOrNull() ?: return
        if(items.isEmpty()) return
        runCatching {
            val id=create()
            save(id,items,"Modelo da instalação anterior")
            legacy.renameTo(File(app.filesDir,"conversation.migrated.json"))
        }
    }
}

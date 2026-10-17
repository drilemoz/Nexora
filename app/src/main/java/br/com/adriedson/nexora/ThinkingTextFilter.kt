package br.com.adriedson.nexora

/** Exibe apenas resposta final. Ignora tags Qwen3 <think> inclusive quando divididas entre tokens. */
class ThinkingTextFilter {
    private val raw = StringBuilder()
    fun append(token: String): String {
        raw.append(token)
        return visibleText(raw.toString(), complete = false)
    }
    fun finish(): String = visibleText(raw.toString(), complete = true)

    companion object {
        fun visibleText(text: String, complete: Boolean = true): String {
            val open = "<think>"
            val close = "</think>"
            var inside = false
            val shown = StringBuilder()
            var i = 0
            while (i < text.length) {
                if (text.startsWith(open, i)) { inside = true; i += open.length; continue }
                if (text.startsWith(close, i)) { inside = false; i += close.length; continue }
                if (!complete && text[i] == '<' && (
                        open.startsWith(text.substring(i)) || close.startsWith(text.substring(i)))) break
                if (!inside) shown.append(text[i])
                i++
            }
            return shown.toString().trimStart()
        }
    }
}

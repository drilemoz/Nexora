package br.com.adriedson.nexora

/** Exibe apenas resposta final. Ignora tags Qwen3 <think> inclusive quando divididas entre tokens. */
class ThinkingTextFilter {
    private val shown = StringBuilder()
    // Apenas prefixos candidatos de <think> e </think>; memoria limitada a 8 caracteres.
    private val prefix = StringBuilder()
    private var inside = false

    /** Recebe fragmentos sem reconstruir toda a saída a cada token. */
    fun accept(token: String) {
        for (char in token) feed(char)
    }

    /** Permite pular renderização enquanto só chegam tokens de raciocínio oculto. */
    fun visibleLength(): Int = shown.length

    /** Chamar apenas quando a interface realmente precisar renderizar. */
    fun visibleText(): String = shown.toString().trimStart()

    // Compatibilidade com consumidores/testes que utilizavam append diretamente.
    fun append(token: String): String {
        accept(token)
        return visibleText()
    }

    fun finish(): String {
        // Um marcador incompleto ao fim do fluxo nao deve apagar texto visivel.
        if (!inside) shown.append(prefix)
        prefix.clear()
        return shown.toString().trimStart()
    }

    private fun feed(char: Char) {
        // Caminho comum: caracteres de texto não participam de tags <think>.
        // Evita criar String temporária para cada letra da resposta.
        if (prefix.isEmpty() && char != '<') {
            if (!inside) shown.append(char)
            return
        }
        prefix.append(char)
        while (prefix.isNotEmpty()) {
            val current = prefix.toString()
            when {
                current == "<think>" -> { inside = true; prefix.clear(); return }
                current == "</think>" -> { inside = false; prefix.clear(); return }
                "<think>".startsWith(current) || "</think>".startsWith(current) -> return
                else -> {
                    if (!inside) shown.append(prefix[0])
                    prefix.deleteCharAt(0)
                }
            }
        }
    }

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

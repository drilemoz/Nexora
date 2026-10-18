package br.com.adriedson.nexora

/** Quantidade de itens antigos que podem ser preservados no começo da nova sequência.
 *  O banco mantém somente o sufixo antigo idêntico ao prefixo desejado; os demais
 *  registros são removidos/gravados numa transação única para não reescrever o histórico.
 */
internal object MessageSaveDiff {
    fun <T> retainedCount(old: List<T>, desired: List<T>): Int {
        for (size in minOf(old.size, desired.size) downTo 1) {
            val offset = old.size - size
            if ((0 until size).all { old[offset + it] == desired[it] }) return size
        }
        return 0
    }
}

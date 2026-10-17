package br.com.adriedson.nexora

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Apenas expressões aritméticas explícitas. Sem avaliar código, funções nem unidades. */
object LocalArithmetic {
    data class Result(val value: String, val approximate: Boolean)
    private val request = Regex(
        "^\\s*(?:quanto(?:\\s+(?:é|e|vale|dá|da|fica))?|calcule|calcular|resolva|resultado\\s+de)\\s*[:?]?\\s*(.+)$",
        RegexOption.IGNORE_CASE
    )
    fun tryCalculate(question: String): Result? = runCatching {
        val source = request.matchEntire(question.trim())?.groupValues?.getOrNull(1)
            ?.trim()?.trimEnd('?')?.trim() ?: return null
        if (source.length !in 3..140 || !source.any { it in "+-*/%" } ||
            !source.all { it.isDigit() || it in " +\u2212-*/%().," }) return null
        val input = source.replace('\u2212', '-').replace(',', '.')
        val parser = Parser(input)
        val value = parser.parse()
        val rounded = value.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros()
        val normal = rounded.toPlainString()
        val parts = normal.split('.')
        val whole = parts[0].reversed().chunked(3).joinToString(".").reversed()
        val formatted = whole + if (parts.size > 1) ",${parts[1]}" else ""
        Result(formatted, parser.approximate || value.compareTo(rounded) != 0)
    }.getOrNull()

    private class Parser(private val source: String) {
        var pos = 0
        var approximate = false
        private val ctx = MathContext.DECIMAL128
        fun parse(): BigDecimal {
            val result = expression()
            skipSpaces()
            require(pos == source.length) { "Sufixo não reconhecido" }
            return result
        }
        private fun skipSpaces() { while (pos < source.length && source[pos].isWhitespace()) pos++ }
        private fun eat(ch: Char): Boolean {
            skipSpaces()
            if (pos < source.length && source[pos] == ch) { pos++; return true }
            return false
        }
        private fun expression(): BigDecimal {
            var left = term()
            while (true) left = when {
                eat('+') -> left.add(term(), ctx)
                eat('-') -> left.subtract(term(), ctx)
                else -> return left
            }
        }
        private fun term(): BigDecimal {
            var left = factor()
            while (true) left = when {
                eat('*') -> left.multiply(factor(), ctx)
                eat('/') -> divide(left, factor())
                else -> return left
            }
        }
        private fun divide(a: BigDecimal, b: BigDecimal): BigDecimal {
            require(b.compareTo(BigDecimal.ZERO) != 0) { "Divisão por zero" }
            return try { a.divide(b) }
            catch (_: ArithmeticException) { approximate = true; a.divide(b, ctx) }
        }
        private fun factor(): BigDecimal {
            val number = when {
                eat('+') -> factor()
                eat('-') -> factor().negate()
                eat('(') -> expression().also { require(eat(')')) { "Parênteses" } }
                else -> number()
            }
            var answer = number
            while (eat('%')) answer = answer.divide(BigDecimal(100))
            return answer
        }
        private fun number(): BigDecimal {
            skipSpaces()
            val start = pos
            while (pos < source.length && (source[pos].isDigit() || source[pos] == '.')) pos++
            require(start < pos) { "Número ausente" }
            return BigDecimal(source.substring(start, pos))
        }
    }
}

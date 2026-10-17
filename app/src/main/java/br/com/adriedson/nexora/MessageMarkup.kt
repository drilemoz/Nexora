package br.com.adriedson.nexora

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan

/** Markdown textual seguro: sem HTML, URLs automáticas, JavaScript ou chamadas externas. */
object MessageMarkup {
    fun render(raw: String): CharSequence {
        if (raw.isBlank()) return "Gerando..."
        val output = SpannableStringBuilder()
        val lines = raw.lines()
        var code = false
        for ((index, original) in lines.withIndex()) {
            if (index > 0) output.append('\n')
            val line = original.trimEnd()
            if (line.trimStart().startsWith("```")) { code = !code; continue }
            if (code) {
                val at=output.length;output.append(line)
                output.setSpan(TypefaceSpan("monospace"),at,output.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                continue
            }
            val heading=Regex("^\\s{0,3}(#{1,6})\\s+").find(line)
            val bullet=Regex("^\\s*([-*+]\\s+|\\d+[.)]\\s+)").find(line)
            val prefix=when { heading!=null -> "";bullet!=null -> "• "; else -> "" }
            val content=when { heading!=null->line.drop(heading.value.length);bullet!=null->line.drop(bullet.value.length);else->line }
            val start=output.length
            output.append(prefix)
            var i=0
            while(i<content.length) {
                val mark=when {content.startsWith("**",i)->"**"; content.startsWith("__",i)->"__";
                    content[i]=='`'->"`";content[i]=='*'->"*";content[i]=='_'->"_";else->null}
                if(mark != null) {
                    val end=content.indexOf(mark,i+mark.length)
                    if(end>i+mark.length) {
                        val begin=output.length;output.append(content.substring(i+mark.length,end))
                        output.setSpan(if(mark=="`" ) TypefaceSpan("monospace") else StyleSpan(if(mark.length==2) Typeface.BOLD else Typeface.ITALIC),begin,output.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i=end+mark.length;continue
                    }
                }
                output.append(content[i]);i++
            }
            if (heading!=null && output.length>start) {
                output.setSpan(StyleSpan(Typeface.BOLD),start,output.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                output.setSpan(RelativeSizeSpan(if(heading.groupValues[1].length<=2) 1.2f else 1.08f),start,output.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return output
    }
}

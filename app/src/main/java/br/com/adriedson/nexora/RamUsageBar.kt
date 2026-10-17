package br.com.adriedson.nexora

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

/** Porcentagem Android disponível = (availMem/totalMem), não necessariamente RAM física ociosa. */
object RamUsageBar {
    fun availablePercent(available: Long, total: Long): Int = ThermalDisplay.availableRamProgress(available,total)
    fun render(context: Context, bar: LinearLayout, available: Long, total: Long) {
        bar.removeAllViews()
        if (total <= 0L) { bar.contentDescription="Medição de RAM indisponível"; return }
        val free = availablePercent(available,total)
        val used = 100-free
        val accent = ThemePreferences.accentColor(context)
        val neutral = ThemePreferences.borderColor(context)
        fun segment(weight: Int, label: String, color: Int, textColor: Int) {
            if (weight == 0) return
            val view = TextView(context).apply {
                text = if (weight>=20) label else ""
                textSize = 10f
                setTextColor(textColor)
                setTypeface(typeface,android.graphics.Typeface.BOLD)
                gravity=Gravity.CENTER
                setBackgroundColor(color)
                setSingleLine(true)
            }
            bar.addView(view, LinearLayout.LayoutParams(0, -1, weight.toFloat()))
        }
        segment(used,"$used% em uso",accent,Color.WHITE)
        segment(free,"$free% livre",neutral,ThemePreferences.labelColor(context))
        bar.contentDescription="$used por cento em uso estimado; $free por cento de RAM disponível"
    }
}

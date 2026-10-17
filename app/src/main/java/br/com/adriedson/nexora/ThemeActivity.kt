package br.com.adriedson.nexora

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

/** Seletor em tela dedicada, sem diálogo de escolha. */
class ThemeActivity : AppCompatActivity() {
    private lateinit var appliedTheme: ThemeOption

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedTheme = ThemePreferences.applyBeforeCreate(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme)
        findViewById<View>(R.id.backFromTheme).setOnClickListener { finish() }
        render()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun render() {
        val container = findViewById<LinearLayout>(R.id.themeOptions)
        container.removeAllViews()
        val selected = ThemePreferences.get(this)
        val descriptions = mapOf(
            ThemeOption.SYSTEM to "A Nexora acompanha a aparência do Android",
            ThemeOption.LIGHT to "Tela branca, contraste suave e cartões claros",
            ThemeOption.DARK to "Azul profundo para uso noturno",
            ThemeOption.AMOLED to "Fundo preto puro para telas OLED"
        )
        ThemeOption.entries.forEach { option ->
            val active = selected == option
            val card = MaterialCardView(this).apply {
                radius = dp(20).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(ThemePreferences.panelColor(this@ThemeActivity))
                strokeColor = if (active) ThemePreferences.accentColor(this@ThemeActivity)
                              else ThemePreferences.borderColor(this@ThemeActivity)
                strokeWidth = dp(if (active) 2 else 1)
                isClickable = true
                isFocusable = true
                contentDescription = "${option.title}: ${if (active) "selecionado" else "selecionar"}"
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
                setOnClickListener {
                    if (ThemePreferences.get(this@ThemeActivity) != option) {
                        ThemePreferences.save(this@ThemeActivity, option)
                        // aplica ao voltar nas Activities anteriores, sem recriar o motor de IA
                        recreate()
                    }
                }
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(18), dp(18), dp(18))
            }
            val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            labels.addView(TextView(this).apply {
                text = option.title
                textSize = 16f
                setTextColor(ThemePreferences.labelColor(this@ThemeActivity))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            labels.addView(TextView(this).apply {
                text = descriptions.getValue(option)
                textSize = 12f
                setTextColor(ThemePreferences.mutedColor(this@ThemeActivity))
                setPadding(0, dp(5), 0, 0)
            })
            row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(this).apply {
                text = if (active) "✓" else "○"
                textSize = 25f
                gravity = Gravity.CENTER
                setTextColor(if (active) ThemePreferences.accentColor(this@ThemeActivity)
                             else ThemePreferences.mutedColor(this@ThemeActivity))
                contentDescription = if (active) "Selecionado" else "Não selecionado"
            }, LinearLayout.LayoutParams(dp(32), dp(44)))
            card.addView(row)
            container.addView(card)
        }
    }
}

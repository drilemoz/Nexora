package br.com.adriedson.nexora

import android.content.Context
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

/** Aplicacao em toda Activity antes de super.onCreate, inclusive na primeira abertura. */
object ThemePreferences {
    private const val FILE = "ui_preferences"
    private const val KEY_THEME = "selected_theme"

    fun get(context: Context): ThemeOption = ThemeOption.fromKey(
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_THEME, null)
    )

    fun save(context: Context, theme: ThemeOption) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, theme.key).apply()
    }

    private fun resolvedColor(context: Context, attr: Int): Int {
        val value = TypedValue()
        check(context.theme.resolveAttribute(attr, value, true)) { "Atributo de tema ausente" }
        return value.data
    }
    fun panelColor(context: Context): Int = resolvedColor(context, R.attr.uiPanel)
    fun labelColor(context: Context): Int = resolvedColor(context, R.attr.uiLabel)
    fun mutedColor(context: Context): Int = resolvedColor(context, R.attr.uiMuted)
    fun accentColor(context: Context): Int = resolvedColor(context, R.attr.uiAccent)
    fun borderColor(context: Context): Int = resolvedColor(context, R.attr.uiBorder)

    fun applyBeforeCreate(activity: AppCompatActivity): ThemeOption {
        val option = get(activity)
        val nightMode = when (option) {
            ThemeOption.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeOption.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeOption.DARK, ThemeOption.AMOLED -> AppCompatDelegate.MODE_NIGHT_YES
        }
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
        if (option == ThemeOption.AMOLED) {
            activity.setTheme(R.style.Theme_Nexora_Amoled)
        }
        return option
    }
}

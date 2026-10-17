package br.com.adriedson.nexora

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var appliedTheme: ThemeOption
    override fun onCreate(savedInstanceState: Bundle?) {
        appliedTheme = ThemePreferences.applyBeforeCreate(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<View>(R.id.backFromSettings).setOnClickListener { finish() }
        findViewById<View>(R.id.chooseTheme).setOnClickListener {
            startActivity(Intent(this, ThemeActivity::class.java))
        }
        findViewById<View>(R.id.manageAiModels).setOnClickListener { startActivity(Intent(this,ModelsActivity::class.java)) }
        findViewById<SwitchCompat>(R.id.autoLoadSwitch).apply {
            isChecked = ModelStartup.enabled(this@SettingsActivity)
            setOnCheckedChangeListener { _, enabled -> ModelStartup.setEnabled(this@SettingsActivity, enabled) }
        }
        findViewById<View>(R.id.autoloadCard).setOnClickListener {
            findViewById<SwitchCompat>(R.id.autoLoadSwitch).apply { isChecked = !isChecked }
        }
        updateLabel()
    }

    override fun onResume() {
        super.onResume()
        if (::appliedTheme.isInitialized && appliedTheme != ThemePreferences.get(this)) {
            recreate()
        } else if (::appliedTheme.isInitialized) updateLabel()
    }

    private fun updateLabel() {
        findViewById<TextView>(R.id.currentTheme).text = ThemePreferences.get(this).title
    }
}

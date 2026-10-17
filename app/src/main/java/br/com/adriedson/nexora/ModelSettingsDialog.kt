package br.com.adriedson.nexora

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.util.Locale

/** Configuração real aplicada na próxima carga. Parâmetros nativos são serializados pelo dispatcher. */
object ModelSettingsDialog {
    fun show(context: Context, file: File) {
        val s=ModelInferencePreferences.get(context,file)
        val outer=ScrollView(context)
        val layout=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(context,22),dp(context,10),dp(context,22),dp(context,10)) }
        outer.addView(layout)
        val controls=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL }
        val profile=TextView(context).apply { text="Perfil atual: ${s.preset}"; textSize=14f; setTextColor(ThemePreferences.accentColor(context)); setPadding(0,0,0,dp(context,12)) }
        layout.addView(profile)
        val state= mutableMapOf<String,Int>()
        val ranges=listOf(
            Range("Contexto (tokens)","ctx", listOf(2048,4096,8192),listOf("2048","4096","8192")),
            Range("Threads CPU","threads",listOf(2,3,4),listOf("2","3","4")),
            Range("Temperatura","temp",(0..15).toList(),(0..15).map{String.format(Locale.US,"%.1f",it/10f)}),
            Range("Top-P","topP",(1..20).toList(),(1..20).map{String.format(Locale.US,"%.2f",it*.05f)}),
            Range("Top-K","topK",(1..20).toList(),(1..20).map{(it*5).toString()}),
            Range("Min-P","minP",(0..20).toList(),(0..20).map{String.format(Locale.US,"%.2f",it*.01f)}),
            Range("Penalidade de repetição","repeat",(0..10).toList(),(0..10).map{String.format(Locale.US,"%.2f",1f+it*.05f)}),
            Range("Limite de resposta (tokens)","tokens",listOf(128,256,512,768,1024,1536),listOf("128","256","512","768","1024","1536"))
        )
        fun initial(range:Range, settings:ModelInferenceSettings):Int {
            val target=when(range.id) {"ctx"->settings.contextSize.toFloat();"threads"->settings.threads.toFloat();
                "temp"->settings.temperature*10; "topP"->settings.topP*20;"topK"->settings.topK/5f;
                "minP"->settings.minP*100;"repeat"->(settings.repeatPenalty-1f)*20;else->settings.maxTokens.toFloat()}
            return range.values.indices.minByOrNull { kotlin.math.abs(range.values[it].toFloat()-target) } ?: 0
        }
        val bars= mutableMapOf<String,SeekBar>()
        fun draw(settings:ModelInferenceSettings) {
            controls.removeAllViews();bars.clear()
            ranges.forEach { range ->
                val pos=initial(range,settings)
                state[range.id]=pos
                val title=TextView(context).apply { text="${range.title}: ${range.labels[pos]}"; setTextColor(ThemePreferences.labelColor(context)); textSize=13f; setPadding(0,dp(context,6),0,0) }
                controls.addView(title)
                val seek=SeekBar(context).apply {
                    max=range.values.size-1;progress=pos
                    setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar:SeekBar?, p:Int,fromUser:Boolean) {
                            state[range.id]=p;title.text="${range.title}: ${range.labels[p]}";
                            if(fromUser) profile.text="Perfil atual: Personalizado"
                        }
                        override fun onStartTrackingTouch(s:SeekBar?)=Unit
                        override fun onStopTrackingTouch(s:SeekBar?)=Unit
                    })
                }
                controls.addView(seek); bars[range.id]=seek
            }
        }
        fun selected():ModelInferenceSettings {
            fun p(k:String)= state[k] ?: 0
            return ModelInferenceSettings.customized(
                ranges[0].values[p("ctx")],ranges[1].values[p("threads")], p("temp")/10f,
                (p("topP")+1)*.05f,(p("topK")+1)*5,p("minP")*.01f,1f+p("repeat")*.05f,
                ranges[7].values[p("tokens")]
            )
        }
        val presetButtons=LinearLayout(context).apply { orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL }
        listOf("Econômico" to ModelInferenceSettings.economical(),"Equilibrado" to ModelInferenceSettings.balanced()).forEach { (label,value) ->
            val b=com.google.android.material.button.MaterialButton(context).apply {
                text=label;textSize=11f;isAllCaps=false;setOnClickListener{profile.text="Perfil atual: $label";draw(value)}
            }
            presetButtons.addView(b,LinearLayout.LayoutParams(0,dp(context,49),1f))
        }
        layout.addView(presetButtons)
        layout.addView(TextView(context).apply { text="O perfil Personalizado é salvo ao alterar qualquer controle e tocar Salvar. Contexto, threads e amostragem são aplicados ao recarregar o modelo."; textSize=11f;setTextColor(ThemePreferences.mutedColor(context)) })
        layout.addView(controls)
        draw(s)
        MaterialAlertDialogBuilder(context).setTitle("Ajustes • ${ModelLabels.display(context,file)}")
            .setView(outer).setNegativeButton("Cancelar",null).setNeutralButton("Restaurar") { _,_ -> ModelInferencePreferences.clear(context,file); Toast.makeText(context,"Padrões restaurados; recarregue o modelo",Toast.LENGTH_LONG).show() }
            .setPositiveButton("Salvar") { _,_ ->
                val chosen=selected()
                val final=when(profile.text.toString().substringAfter(": ")) {
                    "Econômico" -> if (chosen.copy(preset="Econômico")==ModelInferenceSettings.economical()) ModelInferenceSettings.economical() else chosen
                    "Equilibrado" -> if (chosen.copy(preset="Equilibrado")==ModelInferenceSettings.balanced()) ModelInferenceSettings.balanced() else chosen
                    else -> chosen
                }
                ModelInferencePreferences.save(context,file,final)
                Toast.makeText(context,"Ajustes salvos. Descarregue e carregue o modelo para aplicar.",Toast.LENGTH_LONG).show()
            }.show()
    }
    private data class Range(val title:String,val id:String,val values:List<Int>,val labels:List<String>)
    private fun dp(c: Context,n:Int)=(n*c.resources.displayMetrics.density+.5f).toInt()
}

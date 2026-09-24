package app.echo

import android.Manifest
import android.app.*
import android.os.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import app.echo.core.*
import app.echo.platform.*
import java.util.concurrent.Executors

class MainActivity:Activity() {
    private lateinit var content:List<Exercise>
    private lateinit var history:History
    private lateinit var player:Player
    private val microphone=Microphone()
    private val worker=Executors.newSingleThreadExecutor()
    private val handler=Handler(Looper.getMainLooper())
    private var model:OnnxModel?=null
    private var engine:PronunciationEngine?=null
    private var ready=false
    private var preparing=true
    private var busy=false
    private var recording=false
    private var listening=false
    private var selected:Exercise?=null
    private var result:Assessment?=null
    private var previous:Attempt?=null
    private var saved:Attempt?=null
    private var notice=""
    private var page="home"
    private lateinit var column:LinearLayout
    private lateinit var footer:LinearLayout
    private lateinit var container:LinearLayout
    private val preferences by lazy {getSharedPreferences("settings",MODE_PRIVATE)}
    private val dark get()=resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK==Configuration.UI_MODE_NIGHT_YES
    private val bg get()=Color.parseColor(if(dark) "#101B22" else "#F7F5EE")
    private val ink get()=Color.parseColor(if(dark) "#F5F2E8" else "#182B32")
    private val muted get()=Color.parseColor(if(dark) "#B4C1C4" else "#52646A")
    private val accent get()=Color.parseColor(if(dark) "#BCDCCB" else "#245A48")
    private val autoStop=Runnable {if(recording) stopRecording()}
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        content=Content.parse(assets.open("exercises.json").bufferedReader().use {it.readText()})
        history=History(this);player=Player(this)
        page=if(preferences.getBoolean("onboarded",false)) "home" else "welcome"
        render()
        worker.execute {
            try {
                val manager=ModelManager(this);val acoustic=OnnxModel(manager.prepare());model=acoustic
                engine=CtcEngine(acoustic,manager.vocabulary())
                runOnUiThread {if(!isDestroyed) {ready=true;preparing=false;render()}}
            } catch(e:Exception) {runOnUiThread {if(!isDestroyed) {preparing=false;notice="El modelo local no está disponible. Reinstala una build con sus recursos. ${e.javaClass.simpleName}";render()}}}
        }
    }
    private fun base() {
        window.statusBarColor=bg;window.navigationBarColor=bg
        window.decorView.systemUiVisibility=if(dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        container=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(26),dp(16),dp(26),dp(16))}
        val scroll=ScrollView(this).apply {isFillViewport=true;isVerticalScrollBarEnabled=false}
        column=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL}
        scroll.addView(column)
        container.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        footer=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(0,dp(8),0,0)}
        container.addView(footer)
        setContentView(container)
    }
    private fun label(text:String,size:Float=16f,color:Int=ink,target:LinearLayout=column):TextView = TextView(this).apply {
        this.text=text;textSize=size;setTextColor(color);typeface=Typeface.create("sans-serif",Typeface.NORMAL)
        setPadding(0,dp(7),0,dp(7));setLineSpacing(dp(2).toFloat(),1.05f)
        target.addView(this,LinearLayout.LayoutParams(-1,-2))
    }
    private fun space(n:Int=20) {column.addView(View(this),LinearLayout.LayoutParams(1,dp(n)))}
    private fun button(text:String,primary:Boolean=false,target:LinearLayout=footer,enabled:Boolean=true,action:()->Unit):Button {
        return Button(this).apply {
            this.text=text;isAllCaps=false;textSize=17f;minimumHeight=dp(54);isEnabled=enabled
            setTextColor(if(primary) bg else accent)
            background=GradientDrawable().apply {cornerRadius=dp(28).toFloat();setColor(if(primary) accent else Color.TRANSPARENT);if(!primary) setStroke(dp(1),muted)}
            alpha=if(enabled) 1f else 0.45f
            val lp=LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);target.addView(this,lp)
            setOnClickListener {action()}
        }
    }
    private fun render() {
        if(isDestroyed)return
        base()
        label("E C H O",14f,accent)
        when(page) {
            "welcome" -> welcome()
            "home" -> home()
            "practice" -> practice()
            "settings" -> settings()
            "history" -> historyPage()
        }
    }
    private fun welcome() {
        space(50);label("Escucha.\nHabla.\nVuelve a intentarlo.",40f)
        space();label("Tu voz se queda\nen tu teléfono.",24f)
        label("Sin cuenta, sin conexión. Un espacio para practicar inglés a tu ritmo.",17f,muted)
        label("Esta primera versión ofrece una comparación fonética experimental. Puede equivocarse; no es una evaluación de tu nivel de inglés.",15f,muted)
        button("Empezar",true) {preferences.edit().putBoolean("onboarded",true).apply();page="home";render()}
    }
    private fun home() {
        space(24);label("Un momento\npara tu voz.",36f)
        label("24 prácticas · referencia americana",14f,muted)
        if(notice.isNotBlank())label(notice,15f,muted)
        if(preparing)label("Preparando el motor en tu teléfono…",15f,muted)
        val attempts=history.all();val recent=attempts.firstOrNull()
        if(recent!=null)label("${attempts.size} intentos · sigue a tu ritmo",14f,muted)
        var group=""
        content.forEach {exercise ->
            if(exercise.group!=group) {space(16);label(exercise.group.uppercase(),12f,muted);group=exercise.group}
            label(exercise.text,27f).apply {minimumHeight=dp(52);isClickable=true;isFocusable=true;contentDescription="Practicar ${exercise.text}";setOnClickListener {open(exercise)}}
        }
        button(if(recent==null) "Practicar think →" else "Continuar →",true,enabled=ready) {open(content.firstOrNull {it.id==recent?.exercise}?:content[10])}
        button("Mi práctica y ajustes") {page="settings";render()}
    }
    private fun open(exercise:Exercise) {player.stop();listening=false;selected=exercise;result=null;saved=null;notice="";page="practice";render()}
    private fun practice() {
        val e=selected?:return
        label(e.group.uppercase(),12f,muted);space(18)
        if(result==null) {
            label(if(recording) "Te escucho" else if(busy) "Escuchando los detalles…" else "Di en inglés",16f,muted)
            label(e.text,42f)
            space();label(if(recording) "●  Grabando · máximo 12 segundos" else if(busy) "Análisis local. Tu audio permanece aquí." else e.tip,17f,if(recording) accent else muted)
            if(notice.isNotBlank())label(notice,16f,accent)
            if(busy)column.addView(ProgressBar(this),LinearLayout.LayoutParams(dp(36),dp(36)))
            button(if(listening) "Detener referencia" else "Escuchar referencia",enabled=!busy&&!recording) {
                if(listening) {player.stop();listening=false;render()} else {
                    listening=true;render()
                    try {player.playReference(e.id) {listening=false;if(page=="practice")render()}} catch(_:Exception) {listening=false;notice="No se pudo reproducir la referencia.";render()}
                }
            }
            button(if(recording) "■  Terminar" else if(busy) "Analizando…" else "●  Hablar",true,enabled=ready&&!busy&&!listening) {if(recording)stopRecording() else requestRecording()}
        } else {
            val a=result!!
            label(a.score.toString(),76f,accent).contentDescription="Índice experimental ${a.score} de 100"
            label("Similitud fonética · experimental",14f,muted)
            previous?.let {label("Intento anterior ${it.score}  →  ${a.score}",17f,muted)}
            label(e.text,30f)
            val weakest=a.words.minBy {it.score}
            label("Explora «${weakest.word}»",18f,accent)
            a.words.forEach {w ->
                label("${w.word}     ${w.score}   ›",22f).apply {minimumHeight=dp(52);isClickable=true;isFocusable=true;contentDescription="${w.word}, ${w.score}. Ver sonidos";setOnClickListener {showWord(w,e)}}
            }
            label(Feedback.suggestion(weakest,e),17f,muted)
            label("El modelo puede confundir vocales y acentos válidos. Un número mayor indica más coincidencia con este modelo, no un porcentaje de corrección.",14f,muted)
            label("${a.elapsedMs} ms de análisis · sin enviar audio",12f,muted)
            button("Volver a intentarlo",true) {result=null;notice="";render()}
            button("Siguiente →") {open(content[(content.indexOf(e)+1)%content.size])}
        }
        button("Volver",enabled=!busy) {if(recording){microphone.cancel();recording=false;handler.removeCallbacks(autoStop)};player.stop();listening=false;page="home";render()}
    }
    private fun showWord(w:WordScore,e:Exercise) {
        val body=buildString {
            append("Sonidos objetivo y coincidencia experimental:\n\n")
            w.phones.forEach {p ->append("/${p.phone}/    ${p.score}    ${if(p.heard==null) "sin evidencia" else "modelo: /${p.heard}/"}\n")}
            append("\n${Feedback.suggestion(w,e)}\n\nEl alineamiento fuerza una ruta sobre la frase esperada. Estos valores no son diagnósticos. La app no mide aún acento, entonación ni ritmo.")
        }
        AlertDialog.Builder(this).setTitle(w.word).setMessage(body).setPositiveButton("Entendido",null).show()
    }
    private fun requestRecording() {
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),42);return
        }
        try {
            player.stop();microphone.start();recording=true;notice="";result=null
            if(preferences.getBoolean("haptics",true))container.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            handler.postDelayed(autoStop,12000);render()
        }catch(_:Exception){notice="No se pudo abrir el micrófono. Cierra otras apps que lo usen y vuelve a intentar.";render()}
    }
    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==42 && grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED && page=="practice")requestRecording()
        else {notice="Activa el permiso de micrófono para practicar. Puedes seguir escuchando las referencias.";render()}
    }
    private fun stopRecording() {
        if(!recording)return
        recording=false;busy=true;handler.removeCallbacks(autoStop);render()
        val exercise=selected!!
        worker.execute {
            try {
                val samples=microphone.stop();val assessment=engine!!.assess(samples,exercise)
                val prior=history.all().firstOrNull {it.exercise==exercise.id && org.json.JSONObject(it.detail).optString("version")==assessment.version}
                val attempt=history.save(exercise.id,assessment,samples)
                runOnUiThread {if(!isDestroyed){previous=prior;saved=attempt;result=assessment;busy=false;render()}}
            }catch(e:Exception){runOnUiThread {if(!isDestroyed){busy=false;notice=if(e is AudioRejected)e.reason else "No pude completar el análisis. Intenta una frase más corta. ${e.javaClass.simpleName}";render()}}}
        }
    }
    private fun settings() {
        space();label("Tu práctica",36f)
        val all=history.all();val week=all.filter {it.time>System.currentTimeMillis()-7*86400000L}
        label("${String.format(java.util.Locale.ROOT,"%.1f",week.sumOf {it.duration}/60000.0)} min esta semana",23f)
        label("${all.size} intentos guardados",16f,muted)
        label("Compara intentos del mismo ejercicio. Los índices de frases distintas no miden lo mismo.",15f,muted)
        button("Ver intentos y grabaciones",target=column) {page="history";render()}
        val toggle=Switch(this).apply {text="Vibración suave";textSize=17f;setTextColor(ink);minHeight=dp(56);isChecked=preferences.getBoolean("haptics",true);setOnCheckedChangeListener {_,checked ->preferences.edit().putBoolean("haptics",checked).apply()}}
        column.addView(toggle)
        label("Privacidad",24f);label("El micrófono solo se usa al practicar. Grabaciones e historial se guardan en el almacenamiento privado de ECHO. No hay cuenta, anuncios, telemetría ni permiso de Internet. Las copias de seguridad de Android están desactivadas.",16f,muted)
        button("Borrar historial y grabaciones",target=column) {confirmDelete("Se borrarán todos los intentos y audios.") {history.clear();render()}}
        button("Borrar todos mis datos",target=column) {confirmDelete("Se borrarán audios, historial y preferencias. Los recursos incluidos en la app se conservan.") {history.clear();preferences.edit().clear().commit();result=null;previous=null;saved=null;page="welcome";render()}}
        button("Acerca del índice",target=column) {AlertDialog.Builder(this).setTitle("Qué significa 82").setMessage("Es un índice heurístico de coincidencia fonética: 60% evidencia acústica relativa y 40% coincidencia con los fonemas decodificados. Se penalizan omisiones y sonidos extra. No significa 82% correcto ni una probabilidad de dominio.\n\nNo usamos un reconocedor de texto: esta versión no afirma haber entendido tus palabras. El modelo se equivoca incluso con referencias sintéticas. Falta validación con hablantes humanos.").setPositiveButton("Cerrar",null).show()}
        button("Licencias",target=column) {AlertDialog.Builder(this).setTitle("Software y recursos").setMessage(assets.open("NOTICE.txt").bufferedReader().use {it.readText()}).setPositiveButton("Cerrar",null).show()}
        button("Volver") {page="home";render()}
    }
    private fun historyPage() {
        label("Intentos",36f)
        val all=history.all()
        if(all.isEmpty())label("Aquí aparecerá tu primera práctica.",17f,muted)
        all.forEach {attempt ->
            val e=content.first {it.id==attempt.exercise};label("${e.text}  ·  ${attempt.score}",23f)
            label(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(java.util.Date(attempt.time)),13f,muted)
            if(attempt.recording!=null) {
                button("Escuchar mi grabación",target=column) {try {player.playFile(history.file(attempt)!!) {}}catch(_:Exception){Toast.makeText(this,"No se pudo abrir el audio",Toast.LENGTH_SHORT).show()}}
                button("Borrar esta grabación",target=column) {confirmDelete("El audio se borrará. La puntuación se conservará.") {player.stop();history.deleteRecording(attempt);render()}}
            }
        }
        button("Volver") {player.stop();page="settings";render()}
    }
    private fun confirmDelete(message:String,action:()->Unit) {AlertDialog.Builder(this).setTitle("¿Borrar datos?").setMessage(message).setNegativeButton("Cancelar",null).setPositiveButton("Borrar") {_,_->try{action()}catch(_:Exception){Toast.makeText(this,"No se pudo completar el borrado",Toast.LENGTH_LONG).show()}}.show()}
    @Deprecated("Legacy Android back callback")
    override fun onBackPressed() {
        if(busy)return
        if(page=="home"||page=="welcome"){super.onBackPressed();return}
        microphone.cancel();recording=false;player.stop();listening=false;handler.removeCallbacks(autoStop);page="home";render()
    }
    override fun onConfigurationChanged(config:Configuration) {super.onConfigurationChanged(config);render()}
    override fun onStop() {
        super.onStop();player.stop();listening=false
        if(recording){microphone.cancel();recording=false;handler.removeCallbacks(autoStop);notice="La grabación se canceló al salir de ECHO.";render()}
    }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null);microphone.close();player.stop()
        worker.execute {model?.close();history.close()};worker.shutdown();super.onDestroy()
    }
}

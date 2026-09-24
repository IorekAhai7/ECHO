package app.echo.platform
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import app.echo.core.Assessment
import java.io.File
import org.json.JSONObject
import org.json.JSONArray

data class Attempt(val id:Long,val exercise:String,val score:Int,val time:Long,val duration:Long,val recording:String?,val detail:String)
class History(context:Context):SQLiteOpenHelper(context,"echo.db",null,1) {
    private val audio=File(context.filesDir,"recordings").apply {mkdirs()}
    override fun onCreate(db:SQLiteDatabase) {db.execSQL("CREATE TABLE attempts(id INTEGER PRIMARY KEY AUTOINCREMENT, exercise TEXT NOT NULL, score INTEGER NOT NULL, time INTEGER NOT NULL, duration INTEGER NOT NULL, recording TEXT, detail TEXT NOT NULL)")}
    override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int) {error("Migration required")}
    fun save(exercise:String,result:Assessment,samples:FloatArray):Attempt {
        val file=File(audio,"${java.util.UUID.randomUUID()}.wav")
        val detail=JSONObject().put("version",result.version).put("heard",JSONArray(result.heard)).put("additions",result.additions).put("elapsedMs",result.elapsedMs)
        detail.put("words",JSONArray(result.words.map { w -> JSONObject().put("word",w.word).put("score",w.score).put("phones",JSONArray(w.phones.map { p -> JSONObject().put("phone",p.phone).put("score",p.score).put("heard",p.heard?:JSONObject.NULL).put("startMs",p.startMs).put("endMs",p.endMs) })) }))
        val now=System.currentTimeMillis()
        try {
            Wav.write(file,samples)
            val values=ContentValues().apply {put("exercise",exercise);put("score",result.score);put("time",now);put("duration",result.durationMs);put("recording",file.name);put("detail",detail.toString())}
            val id=writableDatabase.insertOrThrow("attempts",null,values)
            return Attempt(id,exercise,result.score,now,result.durationMs,file.name,detail.toString())
        } catch(e:Exception) {file.delete();throw e}
    }
    fun all():List<Attempt> = readableDatabase.rawQuery("SELECT id,exercise,score,time,duration,recording,detail FROM attempts ORDER BY id DESC",null).use {c ->
        buildList {while(c.moveToNext()) add(Attempt(c.getLong(0),c.getString(1),c.getInt(2),c.getLong(3),c.getLong(4),if(c.isNull(5)) null else c.getString(5),c.getString(6)))}
    }
    fun file(attempt:Attempt)=attempt.recording?.let {File(audio,it)}
    fun deleteRecording(attempt:Attempt) {
        file(attempt)?.let {check(!it.exists() || it.delete()) {"No se pudo borrar el audio."}}
        writableDatabase.execSQL("UPDATE attempts SET recording=NULL WHERE id=?",arrayOf(attempt.id))
    }
    fun clear() {
        audio.listFiles()?.forEach {check(it.delete()) {"No se pudo borrar una grabación."}}
        writableDatabase.delete("attempts",null,null)
    }
}

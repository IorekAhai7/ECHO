package app.echo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import app.echo.core.*
import app.echo.platform.*

/** Runs the actual shipped model on bundled audio; microphone needs a human device check. */
@Suppress("DEPRECATION")
class OfflineIntegrationTest {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    @Test fun testOfflineReferenceRetryAndDeletion() {
        val context=instrumentation.targetContext
        val content=Content.parse(context.assets.open("exercises.json").bufferedReader().use{it.readText()})
        val manager=ModelManager(context)
        OnnxModel(manager.prepare()).use {model ->
            val engine=CtcEngine(model,manager.vocabulary())
            val e=content.first{it.id=="e23"}
            val samples=Wav.read(context.assets.open("reference/${e.id}.wav").use{it.readBytes()})
            val first=engine.assess(samples,e);val next=engine.assess(samples,e)
            assertTrue(first.score>65);assertEquals(first.score,next.score);assertEquals(e.words.size,first.words.size)
            val wrong=engine.assess(Wav.read(context.assets.open("reference/e02.wav").use{it.readBytes()}),e)
            assertTrue(wrong.score<first.score)
            // Isolated database/files namespace: never erase user practice data during tests.
            val isolated=object:android.content.ContextWrapper(context) {
                override fun getDatabasePath(name:String)=super.getDatabasePath("test-"+name)
                override fun openOrCreateDatabase(name:String,mode:Int,factory:android.database.sqlite.SQLiteDatabase.CursorFactory?)=super.openOrCreateDatabase("test-"+name,mode,factory)
                override fun openOrCreateDatabase(name:String,mode:Int,factory:android.database.sqlite.SQLiteDatabase.CursorFactory?,errorHandler:android.database.DatabaseErrorHandler?)=super.openOrCreateDatabase("test-"+name,mode,factory,errorHandler)
                override fun getFilesDir()=java.io.File(super.getFilesDir(),"integration-test").apply{mkdirs()}
            }
            History(isolated).use {history ->
                history.clear();val a=history.save(e.id,first,samples);val b=history.save(e.id,next,samples)
                assertEquals(2,history.all().size);assertTrue(history.file(a)!!.exists());assertTrue(b.id>a.id)
                history.deleteRecording(a);assertFalse(history.file(a)!!.exists());assertNull(history.all().last().recording)
            }
            History(isolated).use {history ->assertEquals(2,history.all().size);history.clear();assertEquals(0,history.all().size)}
            android.util.Log.i("ECHO_BENCH","score=${first.score}; analysisMs=${first.elapsedMs}; wrong=${wrong.score}; durationMs=${first.durationMs}")
        }
        assertEquals(android.content.pm.PackageManager.PERMISSION_DENIED,context.packageManager.checkPermission("android.permission.INTERNET",context.packageName))
    }
}

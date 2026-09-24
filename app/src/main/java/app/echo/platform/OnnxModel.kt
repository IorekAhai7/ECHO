package app.echo.platform
import android.content.Context
import ai.onnxruntime.*
import app.echo.core.*
import org.json.JSONObject
import java.io.File
import java.nio.FloatBuffer

class ModelManager(private val context: Context) {
    fun prepare(): File {
        val destination=File(context.noBackupFilesDir,"phonemes-v1.onnx")
        val expected=context.assets.openFd("models/model.int8.onnx").use { it.length }
        if(!destination.exists() || destination.length()!=expected) {
            val temporary=File(context.noBackupFilesDir,"model.tmp")
            context.assets.open("models/model.int8.onnx").use { input -> temporary.outputStream().use { input.copyTo(it) } }
            check(temporary.length()==expected && temporary.renameTo(destination)) { "No se pudo preparar el modelo local." }
        }
        return destination
    }
    fun vocabulary(): Map<String,Int> {
        val obj=JSONObject(context.assets.open("models/vocab.json").bufferedReader().use { it.readText() }).getJSONObject("id_to_phoneme")
        return obj.keys().asSequence().associate { obj.getString(it) to it.toInt() }
    }
}
class OnnxModel(file: File) : AcousticModel, AutoCloseable {
    private val env=OrtEnvironment.getEnvironment()
    private val session: OrtSession
    init {
        env.setTelemetry(false)
        OrtSession.SessionOptions().use { options ->
            options.setIntraOpNumThreads(2)
            session=env.createSession(file.absolutePath,options)
        }
    }
    @Suppress("UNCHECKED_CAST")
    override fun infer(normalizedSamples: FloatArray): Array<FloatArray> {
        OnnxTensor.createTensor(env,FloatBuffer.wrap(normalizedSamples),longArrayOf(1,normalizedSamples.size.toLong())).use { input ->
            session.run(mapOf("input_values" to input)).use { output ->
                return (output[0].value as Array<Array<FloatArray>>)[0]
            }
        }
    }
    override fun close() { session.close() }
}

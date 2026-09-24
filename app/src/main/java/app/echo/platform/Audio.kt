package app.echo.platform
import android.media.*
import android.content.Context
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import java.util.concurrent.Future

interface AudioCapture { fun start(); fun stop(): FloatArray; fun cancel() }
class Microphone : AudioCapture {
    private var recorder: AudioRecord?=null
    @Volatile private var running=false
    private var job: Future<FloatArray>?=null
    private val executor=Executors.newSingleThreadExecutor()
    @Suppress("MissingPermission")
    @Synchronized override fun start() {
        check(!running)
        val size=maxOf(4096,AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT))
        val record=AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size)
        if(record.state!=AudioRecord.STATE_INITIALIZED) {record.release();error("No se pudo abrir el micrófono.")}
        recorder=record;running=true;record.startRecording()
        job=executor.submit<FloatArray> {
            val pcm=FloatArray(16000*12);val buffer=ShortArray(1024);var count=0
            while(running && count<pcm.size) {
                val n=record.read(buffer,0,minOf(buffer.size,pcm.size-count))
                if(n<0) { if(running) error("No se pudo leer el micrófono.") else break }
                for(i in 0 until n) pcm[count++]=buffer[i]/32768f
            }
            pcm.copyOf(count)
        }
    }
    @Synchronized override fun stop(): FloatArray {
        running=false
        return try { recorder?.stop();job?.get()?:floatArrayOf() } finally {recorder?.release();recorder=null;job=null}
    }
    @Synchronized override fun cancel() { if(recorder!=null) runCatching {stop()} }
    fun close() {cancel();executor.shutdown()}
}
interface AudioPlayback { fun playReference(id: String, onDone: ()->Unit); fun stop() }
class Player(private val context: Context) : AudioPlayback {
    private var media: MediaPlayer?=null
    override fun stop() {media?.release();media=null}
    override fun playReference(id: String,onDone: ()->Unit) {
        stop()
        val p=MediaPlayer();media=p
        context.assets.openFd("reference/$id.wav").use {p.setDataSource(it.fileDescriptor,it.startOffset,it.length)}
        p.setOnCompletionListener {stop();onDone()}
        p.setOnErrorListener { _,_,_ ->stop();onDone();true}
        p.prepare();p.start()
    }
    fun playFile(file: File,onDone: ()->Unit) {
        stop();val p=MediaPlayer();media=p;p.setDataSource(file.absolutePath)
        p.setOnCompletionListener {stop();onDone()};p.prepare();p.start()
    }
}
object Wav {
    fun write(file: File,samples: FloatArray) {
        val b=ByteBuffer.allocate(44+samples.size*2).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray());b.putInt(36+samples.size*2);b.put("WAVEfmt ".toByteArray());b.putInt(16)
        b.putShort(1);b.putShort(1);b.putInt(16000);b.putInt(32000);b.putShort(2);b.putShort(16);b.put("data".toByteArray());b.putInt(samples.size*2)
        samples.forEach {b.putShort((it.coerceIn(-1f,1f)*32767).toInt().toShort())};file.writeBytes(b.array())
    }
    /** PCM16 mono WAV reader for fixtures. Allows unknown chunks and resamples to 16k. */
    fun read(bytes: ByteArray): FloatArray {
        val b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(String(bytes,0,4)=="RIFF" && String(bytes,8,4)=="WAVE")
        var offset=12;var rate=0
        while(offset+8<=bytes.size) {
            val tag=String(bytes,offset,4);val size=b.getInt(offset+4);require(size>=0 && offset+8L+size<=bytes.size)
            if(tag=="fmt ") { require(b.getShort(offset+8).toInt()==1 && b.getShort(offset+10).toInt()==1 && b.getShort(offset+22).toInt()==16);rate=b.getInt(offset+12) }
            if(tag=="data") {
                require(rate>0)
                val x=FloatArray(size/2) {b.getShort(offset+8+it*2)/32768f}
                return FloatArray((x.size*16000L/rate).toInt()) { i ->
                    val at=i*rate/16000.0;val lo=at.toInt().coerceAtMost(x.lastIndex);val hi=minOf(lo+1,x.lastIndex)
                    (x[lo]+(x[hi]-x[lo])*(at-lo)).toFloat()
                }
            }
            offset+=8+size+(size%2)
        }
        error("No PCM data")
    }
}

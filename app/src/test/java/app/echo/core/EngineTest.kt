package app.echo.core
import org.junit.Test
import org.junit.Assert.*
import app.echo.platform.Content
import app.echo.platform.Wav
import java.io.File
import kotlin.math.*

class EngineTest {
    private val vocabulary=mapOf("<pad>" to 0,"<s>" to 1,"</s>" to 2,"<unk>" to 3,"a" to 4,"b" to 5,"c" to 6)
    private val exercise=Exercise("test","ab","Test","Tip",listOf(Word("a",listOf("a")),Word("b",listOf("b"))))
    private fun samples()=FloatArray(16000) { (0.1*sin(it*0.05)).toFloat() }
    private fun engine(ids:List<Int>)=CtcEngine(object:AcousticModel {override fun infer(normalizedSamples:FloatArray)=ids.map { id ->FloatArray(7) {if(it==id) 8f else -8f}}.toTypedArray()},vocabulary)
    @Test fun referenceScoresHigh() {val r=engine(listOf(0,4,4,0,5,5,0)).assess(samples(),exercise);assertEquals(100,r.score);assertEquals(2,r.words.size)}
    @Test fun wrongPhoneScoresLower() {val r=engine(listOf(0,4,0,6,0)).assess(samples(),exercise);assertTrue(r.score<70);assertTrue(r.words[1].score<10)}
    @Test fun omissionHasNoScoreEvidence() {val r=engine(listOf(0,4,4,0,0)).assess(samples(),exercise);assertEquals(0,r.words[1].score)}
    @Test fun additionPenalizesOverall() {val r=engine(listOf(0,4,0,5,0,6,0)).assess(samples(),exercise);assertEquals(1,r.additions);assertTrue(r.score<100)}
    @Test fun repeatsAreDeterministic() {val e=engine(listOf(0,4,0,5,0));assertEquals(e.assess(samples(),exercise).score,e.assess(samples(),exercise).score)}
    @Test fun repeatedPhoneRequiresBlank() {
        val logits=arrayOf(floatArrayOf(0f,3f),floatArrayOf(3f,0f),floatArrayOf(0f,3f))
        val path=CtcAlignment.align(logits,listOf(1,1));assertEquals(listOf(0),path[0]);assertEquals(listOf(2),path[1])
    }
    @Test(expected=AudioRejected::class) fun impossibleRepeatedPathRejected() {CtcAlignment.align(arrayOf(floatArrayOf(0f,3f),floatArrayOf(0f,3f)),listOf(1,1))}
    @Test fun editAlignmentOmitsMiddleWord() {val a=PhoneAlignment.align(listOf(4,5,6),listOf(4,6));assertEquals(listOf(4,null,6),a.observed)}
    @Test fun editAlignmentHandlesEmpty() {assertEquals(listOf(null,null),PhoneAlignment.align(listOf(4,5),emptyList()).observed)}
    @Test(expected=AudioRejected::class) fun silenceRejected() {Signal.validate(FloatArray(16000))}
    @Test(expected=AudioRejected::class) fun shortRejected() {Signal.validate(FloatArray(100))}
    @Test(expected=AudioRejected::class) fun lowVolumeRejected() {Signal.validate(FloatArray(16000) {(0.004*sin(it*0.05)).toFloat()})}
    @Test(expected=AudioRejected::class) fun whiteNoiseRejected() {val random=java.util.Random(5);Signal.validate(FloatArray(16000) {(random.nextFloat()-0.5f)*0.4f})}
    @Test(expected=AudioRejected::class) fun clippingRejected() {Signal.validate(FloatArray(16000) {if(it%2==0)1f else -1f})}
    @Test(expected=AudioRejected::class) fun invalidSamplesRejected() {Signal.validate(FloatArray(16000) {Float.NaN})}
    @Test fun normalizationIsCentered() {assertTrue(abs(Signal.normalize(samples()).average())<1e-6)}
    @Test fun allContentHasAudioAndKnownPhones() {
        val root=File("src/main/assets");val content=Content.parse(File(root,"exercises.json").readText())
        val vocab=org.json.JSONObject(File(root,"models/vocab.json").readText()).getJSONObject("id_to_phoneme")
        val phones=vocab.keys().asSequence().map {vocab.getString(it)}.toSet()
        assertEquals(24,content.size)
        content.forEach {e ->assertTrue(e.tip.isNotBlank());assertTrue(e.words.flatMap {it.phones}.all {it in phones});assertTrue(Wav.read(File(root,"reference/${e.id}.wav").readBytes()).size>4000)}
    }
    @Test fun wavRoundTrip() {val file=File.createTempFile("echo", ".wav");try{val x=samples();Wav.write(file,x);val y=Wav.read(file.readBytes());assertEquals(x.size,y.size);assertTrue(x.indices.all {abs(x[it]-y[it])<0.0001})}finally{file.delete()}}
}

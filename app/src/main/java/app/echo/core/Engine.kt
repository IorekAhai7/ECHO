package app.echo.core

import kotlin.math.*

data class Word(val text: String, val phones: List<String>)
data class Exercise(val id: String, val text: String, val group: String, val tip: String, val words: List<Word>)
data class PhoneScore(val phone: String, val score: Int, val heard: String?, val startMs: Int, val endMs: Int)
data class WordScore(val word: String, val score: Int, val phones: List<PhoneScore>)
data class Assessment(val score: Int, val words: List<WordScore>, val heard: List<String>, val additions: Int, val elapsedMs: Long, val durationMs: Long, val version: String = "ctc-relative-v1")
class AudioRejected(val reason: String) : Exception(reason)
interface PronunciationEngine { fun assess(samples: FloatArray, exercise: Exercise): Assessment }
interface AcousticModel { fun infer(normalizedSamples: FloatArray): Array<FloatArray> }

object Signal {
    fun validate(x: FloatArray) {
        if (x.size < 4000) throw AudioRejected("La grabación es demasiado corta. Habla y espera un momento antes de detener.")
        if (x.size > 16000 * 12) throw AudioRejected("Practica en fragmentos de hasta 12 segundos.")
        if (x.any { !it.isFinite() }) throw AudioRejected("No se pudo leer el audio.")
        val rms = sqrt(x.sumOf { it.toDouble() * it } / x.size)
        if (rms < 0.001) throw AudioRejected("No detecté voz. Acerca el teléfono y vuelve a intentarlo.")
        if (rms < 0.006) throw AudioRejected("El volumen es muy bajo. Acerca un poco el micrófono.")
        if (x.count { abs(it) > 0.99 } > x.size * 0.03) throw AudioRejected("El audio está saturado. Aleja un poco el teléfono.")
        val zcr = (1 until x.size).count { x[it] * x[it-1] < 0 }.toDouble() / x.size
        if (zcr > 0.40) throw AudioRejected("Hay demasiado ruido para analizar con confianza. Busca un lugar más tranquilo.")
    }
    fun normalize(x: FloatArray): FloatArray {
        val mean = x.average(); val variance = x.sumOf { (it-mean).pow(2) } / x.size
        return FloatArray(x.size) { ((x[it] - mean) / sqrt(variance + 1e-7)).toFloat() }
    }
}

/** Global edit alignment of independently decoded phone tokens. null = omitted phone. */
object PhoneAlignment {
    data class EditResult(val observed: List<Int?>, val additions: Int)
    fun align(expected: List<Int>, heard: List<Int>): EditResult {
        val d = Array(expected.size+1) { IntArray(heard.size+1) }
        for (i in d.indices) d[i][0]=i
        for (j in d[0].indices) d[0][j]=j
        for (i in 1..expected.size) for (j in 1..heard.size) {
            d[i][j]=minOf(d[i-1][j]+1,d[i][j-1]+1,d[i-1][j-1]+if(expected[i-1]==heard[j-1]) 0 else 1)
        }
        var i=expected.size;var j=heard.size;var additions=0
        val result=MutableList<Int?>(i) { null }
        while(i>0 || j>0) {
            if(i>0 && j>0 && d[i][j]==d[i-1][j-1]+if(expected[i-1]==heard[j-1]) 0 else 1) {
                result[i-1]=heard[j-1];i--;j--
            } else if(i>0 && d[i][j]==d[i-1][j]+1) i--
            else {additions++;j--}
        }
        return EditResult(result,additions)
    }
}

/** CTC Viterbi alignment including blank states and repeated-token constraints. */
object CtcAlignment {
    fun align(logits: Array<FloatArray>, target: List<Int>, blank: Int=0): List<List<Int>> {
        require(target.isNotEmpty() && logits.isNotEmpty())
        val states=IntArray(target.size*2+1) { if(it%2==0) blank else target[it/2] }
        val trace=Array(logits.size) { IntArray(states.size) { -1 } }
        var prev=DoubleArray(states.size) { Double.NEGATIVE_INFINITY }
        prev[0]=logits[0][blank].toDouble();prev[1]=logits[0][target[0]].toDouble()
        for(t in 1 until logits.size) {
            val next=DoubleArray(states.size) { Double.NEGATIVE_INFINITY }
            for(s in states.indices) {
                var best=s
                if(s>0 && prev[s-1]>prev[best]) best=s-1
                if(s>1 && states[s]!=blank && states[s]!=states[s-2] && prev[s-2]>prev[best]) best=s-2
                next[s]=prev[best]+logits[t][states[s]];trace[t][s]=best
            }
            prev=next
        }
        var state=if(prev.last()>prev[prev.lastIndex-1]) prev.lastIndex else prev.lastIndex-1
        if(!prev[state].isFinite()) throw AudioRejected("No pude alinear la frase completa. Vuelve a decirla despacio.")
        val frames=List(target.size) { mutableListOf<Int>() }
        for(t in logits.lastIndex downTo 0) {
            if(state%2==1) frames[state/2].add(t)
            if(t>0) state=trace[t][state]
        }
        return frames.map { it.reversed() }
    }
}

class CtcEngine(private val model: AcousticModel, private val vocab: Map<String,Int>) : PronunciationEngine {
    private val labels=vocab.entries.associate { it.value to it.key }
    override fun assess(samples: FloatArray, exercise: Exercise): Assessment {
        Signal.validate(samples)
        val started=System.nanoTime()
        val logits=model.infer(Signal.normalize(samples))
        require(logits.isNotEmpty() && logits.all { row -> row.size==vocab.size && row.all { it.isFinite() } })
        val phones=exercise.words.flatMap { it.phones }
        val target=phones.map { requireNotNull(vocab[it]) { "Unsupported phone: $it" } }
        val heard=mutableListOf<Int>();var last=-1
        for(row in logits) {
            val best=row.indices.maxBy { row[it] }
            if(best!=last && best>3) heard.add(best)
            last=best
        }
        if(heard.size<maxOf(1,target.size/5)) throw AudioRejected("No hay suficiente voz reconocible. Repite la frase en un lugar tranquilo.")
        val edits=PhoneAlignment.align(target,heard)
        val frames=CtcAlignment.align(logits,target)
        val frameMs=samples.size/16.0/logits.size
        val scores=target.indices.map { n ->
            // Compare target against the strongest competing non-special phone at each aligned frame.
            // Frame-wise softmax denominator cancels in this posterior ratio. This is NOT calibrated GOP.
            val relative=frames[n].map { t ->
                val maxPhone=(4 until logits[t].size).maxOf { logits[t][it] }
                exp((logits[t][target[n]]-maxPhone).toDouble()).coerceIn(0.0,1.0)
            }.maxOrNull() ?: 0.0
            val match=if(edits.observed[n]==target[n]) 1.0 else 0.0
            val value=if(edits.observed[n]==null) 0 else (100*(0.6*relative+0.4*match)).roundToInt()
            PhoneScore(phones[n],value,edits.observed[n]?.let { labels[it] },((frames[n].firstOrNull()?:0)*frameMs).roundToInt(),(((frames[n].lastOrNull()?:0)+1)*frameMs).roundToInt())
        }
        var offset=0
        val words=exercise.words.map { word ->
            val part=scores.subList(offset,offset+word.phones.size);offset+=part.size
            WordScore(word.text,part.map { it.score }.average().roundToInt(),part)
        }
        val penalty=target.size.toDouble()/(target.size+edits.additions)
        return Assessment((scores.map { it.score }.average()*penalty).roundToInt(),words,heard.map { labels[it]?:"?" },edits.additions,(System.nanoTime()-started)/1_000_000,samples.size/16L)
    }
}

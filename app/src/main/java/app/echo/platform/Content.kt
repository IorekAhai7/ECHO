package app.echo.platform
import app.echo.core.*
import org.json.JSONArray
object Content {
    fun parse(json: String): List<Exercise> {
        val list=JSONArray(json)
        return (0 until list.length()).map { i ->
            val e=list.getJSONObject(i);val words=e.getJSONArray("words")
            Exercise(e.getString("id"),e.getString("text"),e.getString("group"),e.getString("tip"),(0 until words.length()).map { j ->
                val w=words.getJSONObject(j);val p=w.getJSONArray("phones")
                Word(w.getString("text"),(0 until p.length()).map { p.getString(it) })
            })
        }.also { require(it.map { e -> e.id }.distinct().size==it.size);require(it.all { e -> e.words.isNotEmpty() && e.words.all { w -> w.phones.isNotEmpty() } }) }
    }
}

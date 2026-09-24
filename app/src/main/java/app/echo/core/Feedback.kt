package app.echo.core

object Feedback {
    fun forPhone(phone: String, fallback: String): String = when(phone) {
        "ɪ" -> "Relaja la lengua y abre un poco la boca para /ɪ/. Compárala con la vocal más tensa de sheep. La duración por sí sola no distingue estas vocales."
        "iː" -> "Para /iː/, eleva la lengua y mantén la vocal sin deslizarla. Contrasta sheep con ship."
        "v" -> "Apoya los dientes superiores sobre el labio inferior y deja pasar aire con voz para /v/. No cierres ambos labios."
        "b" -> "Cierra ambos labios y ábrelos con voz para /b/. Compáralo con el roce continuo de /v/."
        "θ" -> "Asoma ligeramente la punta de la lengua entre los dientes y deja salir aire sin voz para /θ/."
        "ð" -> "Para /ð/, mantén la lengua junto a los dientes y deja vibrar la voz. Evita cerrar por completo el paso de aire."
        "h" -> "Deja salir aire suavemente para /h/, como al empañar un cristal. Evita una jota fuerte."
        "ə","ɐ" -> "Relaja la boca para la vocal débil /ə/. Es breve y sin énfasis; no la fuerces a sonar como una vocal española clara."
        "æ" -> "Para /æ/, abre la mandíbula y lleva la lengua hacia delante. Escucha bat y compáralo con but."
        "ɹ","ɚ" -> "La r americana no golpea el paladar como la r española. Retrae o agrupa la lengua sin hacerla vibrar."
        else -> fallback
    }
    fun suggestion(word: WordScore, exercise: Exercise): String {
        val phone=word.phones.minBy {it.score}
        return "El modelo encontró menos coincidencia en /${phone.phone}/. Puede ser un error del modelo.\n\n"+forPhone(phone.phone,exercise.tip)
    }
}

package pt.antares.app.core.network.off

object ServingParse {

    const val MIN_GRAMS = 1.0

    const val MAX_GRAMS = 2000.0

    const val MAX_LABEL_LENGTH = 40

    data class Serving(val name: String?, val grams: Double?)

    fun from(servingQuantity: String?, servingSize: String?): Serving {
        val numero = grams(servingQuantity) ?: gramsFromText(servingSize)
        return Serving(name = label(servingSize), grams = numero)
    }

    fun grams(raw: String?): Double? {
        val valor = raw?.trim()?.replace(',', '.')?.toDoubleOrNull() ?: return null
        return valor.takeIf { it >= MIN_GRAMS && it <= MAX_GRAMS }
    }

    fun gramsFromText(raw: String?): Double? {
        val texto = raw?.lowercase()?.replace(',', '.') ?: return null
        val padrao = Regex("""(\d+(?:\.\d+)?)\s*(g|ml)\b""")
        val ultimo = padrao.findAll(texto).lastOrNull() ?: return null
        return grams(ultimo.groupValues[1])
    }

    fun label(servingSize: String?): String? {
        val texto = servingSize?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (texto.length > MAX_LABEL_LENGTH) return null

        val semMedidas = texto
            .replace(Regex("""\(?\s*\d+(?:[.,]\d+)?\s*(g|ml|gr|gramas)\b\s*\)?""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""[(),.]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (semMedidas.isBlank()) return null

        if (semMedidas.none { it.isLetter() }) return null
        return semMedidas
    }
}

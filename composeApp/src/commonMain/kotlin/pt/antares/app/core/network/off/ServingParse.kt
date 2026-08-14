package pt.antares.app.core.network.off

/**
 * Tira uma dose utilizável do que a Open Food Facts traz. Os campos de dose são texto livre
 * escrito por voluntários — "1 chávena (240 ml)", "30", "2 bolachas" — e não há garantia
 * nenhuma de formato.
 */
object ServingParse {

    // Fora deste intervalo é engano de unidade ou de vírgula, não uma dose.
    const val MIN_GRAMS = 1.0

    const val MAX_GRAMS = 2000.0

    // Textos mais longos do que isto são descrições e não nomes de dose; num botão ficariam
    // cortados a meio.
    const val MAX_LABEL_LENGTH = 40

    data class Serving(val name: String?, val grams: Double?)

    // O campo numérico primeiro; só se ele faltar se vai procurar um número dentro do
    // texto. O nome vem sempre do texto, porque é a parte que uma pessoa reconhece.
    fun from(servingQuantity: String?, servingSize: String?): Serving {
        val numero = grams(servingQuantity) ?: gramsFromText(servingSize)
        return Serving(name = label(servingSize), grams = numero)
    }

    // A vírgula vira ponto: os produtos europeus escrevem o decimal à maneira de cá.
    fun grams(raw: String?): Double? {
        val valor = raw?.trim()?.replace(',', '.')?.toDoubleOrNull() ?: return null
        return valor.takeIf { it >= MIN_GRAMS && it <= MAX_GRAMS }
    }

    fun gramsFromText(raw: String?): Double? {
        val texto = raw?.lowercase()?.replace(',', '.') ?: return null
        val padrao = Regex("""(\d+(?:\.\d+)?)\s*(g|ml)\b""")
        // O último número com unidade, e não o primeiro: em "2 bolachas (30 g)" o primeiro
        // é a contagem e é o segundo que dá o peso.
        val ultimo = padrao.findAll(texto).lastOrNull() ?: return null
        return grams(ultimo.groupValues[1])
    }

    fun label(servingSize: String?): String? {
        val texto = servingSize?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (texto.length > MAX_LABEL_LENGTH) return null

        // Tira o peso do texto — a app já o mostra ao lado — e sobra o nome: de
        // "1 chávena (240 ml)" fica "1 chávena".
        val semMedidas = texto
            .replace(Regex("""\(?\s*\d+(?:[.,]\d+)?\s*(g|ml|gr|gramas)\b\s*\)?""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""[(),.]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (semMedidas.isBlank()) return null

        // Sem letras nenhumas sobrou só um número, que não é nome de dose nenhum.
        if (semMedidas.none { it.isLetter() }) return null
        return semMedidas
    }
}

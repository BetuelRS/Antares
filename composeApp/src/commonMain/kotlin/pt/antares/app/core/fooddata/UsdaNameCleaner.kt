package pt.antares.app.core.fooddata

/**
 * Torna legíveis os nomes da tabela americana. Lá vêm escritos para catalogar e não para
 * ler — "RICE,WHITE,LONG-GRAIN,REGULAR,RAW,UNENRICHED" — e assim são impossíveis de
 * encontrar numa lista.
 */
object UsdaNameCleaner {

    // Partes que só existem para o catálogo e não dizem nada a quem procura comida.
    private val DROP_PARTS = setOf(
        "all commercial varieties", "commercial", "nfs", "ns as to form",
        "unprepared", "includes usda commodity", "raw",
    )

    // Adjetivos que a tabela põe depois da vírgula e que em português vêm antes: "Rice,
    // white" tem de virar "White rice" para se ler como um nome.
    private val LEADING_ADJECTIVES = setOf(
        "white", "brown", "red", "green", "yellow", "black", "wild",
        "cooked", "boiled", "roasted", "fried", "grilled", "baked", "steamed",
        "dried", "fresh", "frozen", "canned", "smoked", "cured",
        "whole", "skim", "nonfat", "low-fat", "lowfat", "reduced-fat", "fat-free",
        "lean", "sweet", "sour", "unsweetened", "sweetened", "salted", "unsalted",
        "ripe", "young", "mature", "ground", "shredded", "sliced", "chopped",
    )

    fun clean(raw: String): String {

        // Tudo a partir de "(includes" é a lista de códigos equivalentes e pode ser mais
        // comprida do que o nome.
        val noBoiler = run {
            val i = raw.lowercase().indexOf("(includes")
            if (i >= 0) raw.substring(0, i).trim() else raw.trim()
        }
        var parts = noBoiler.split(',').map { it.trim() }.filter { it.isNotEmpty() }

        parts = parts.filter { it.lowercase() !in DROP_PARTS }
        // Se a limpeza não deixou nada, devolve-se o original: um nome feio é melhor do que
        // um alimento sem nome nenhum.
        if (parts.isEmpty()) return raw.trim()

        parts = parts.map(::deshout)

        val ordered = if (parts.size >= 2 && parts[1].lowercase() in LEADING_ADJECTIVES) {
            val base = parts[0].lowercase()
            val adj = parts[1].lowercase()
            listOf("$adj $base") + parts.drop(2)
        } else {
            parts
        }

        return ordered.joinToString(", ").replaceFirstChar { it.uppercaseChar() }
    }

    /**
     * Baixa as palavras escritas todas em maiúsculas. Exige duas letras para não estragar
     * siglas de uma letra nem unidades como "C" de vitamina.
     */
    private fun deshout(part: String): String =
        part.split(' ').joinToString(" ") { word ->
            val letters = word.count { it.isLetter() }
            if (letters >= 2 && word.none { it.isLetter() && it.isLowerCase() }) word.lowercase() else word
        }
}

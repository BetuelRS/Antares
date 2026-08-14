package pt.antares.app.core.util

/**
 * Transforma o que a pessoa escreve numa expressão que o FTS4 do SQLite entende. O texto
 * cru não serve: pontuação e acentos partem a sintaxe da pesquisa.
 */
object FtsQuery {

    // Preposições e artigos dos dois idiomas do catálogo. "Arroz com feijão" tem de
    // encontrar o mesmo que "arroz feijão", e o "com" só reduziria os resultados.
    private val stopWords = setOf(
        "com", "de", "do", "da", "dos", "das", "sem", "ao", "aos", "no", "na",
        "nos", "nas", "em", "ou", "para", "por", "um", "uma", "uns", "umas",
        "and", "with", "of", "the", "for",
    )

    // O asterisco em cada palavra faz a pesquisa acontecer enquanto se escreve: "fran" já
    // encontra frango, sem ser preciso acabar a palavra.
    fun build(raw: String): String =
        tokens(raw).joinToString(" ") { "$it*" }

    fun tokens(raw: String): List<String> {
        val normalized = TextNormalize.normalize(raw.trim())
        // Corta em tudo o que não seja letra ou número — o texto já vem sem acentos — e
        // deita fora palavras de uma letra, que devolveriam meio catálogo.
        val tokens = normalized.split(Regex("[^a-z0-9]+")).filter { it.length >= 2 }
        if (tokens.isEmpty()) return emptyList()
        val meaningful = tokens.filter { it !in stopWords }

        // Se a pesquisa for só palavras vazias, usam-se elas mesmas: mais vale procurar
        // "com" do que não procurar nada e devolver o catálogo inteiro.
        return meaningful.ifEmpty { tokens }
    }
}

package pt.antares.app.core.util

object FtsQuery {

    private val stopWords = setOf(
        "com", "de", "do", "da", "dos", "das", "sem", "ao", "aos", "no", "na",
        "nos", "nas", "em", "ou", "para", "por", "um", "uma", "uns", "umas",
        "and", "with", "of", "the", "for",
    )

    fun build(raw: String): String =
        tokens(raw).joinToString(" ") { "$it*" }

    fun tokens(raw: String): List<String> {
        val normalized = TextNormalize.normalize(raw.trim())
        val tokens = normalized.split(Regex("[^a-z0-9]+")).filter { it.length >= 2 }
        if (tokens.isEmpty()) return emptyList()
        val meaningful = tokens.filter { it !in stopWords }

        return meaningful.ifEmpty { tokens }
    }
}

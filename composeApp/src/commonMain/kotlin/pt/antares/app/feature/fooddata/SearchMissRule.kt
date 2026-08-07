package pt.antares.app.feature.fooddata

import pt.antares.app.core.util.TextNormalize

object SearchMissRule {

    const val MIN_QUERY_LENGTH = 3

    const val MAX_QUERY_LENGTH = 60

    fun normalize(raw: String): String? {
        val limpo = TextNormalize.normalize(raw).trim()
        if (limpo.length < MIN_QUERY_LENGTH || limpo.length > MAX_QUERY_LENGTH) return null

        if (limpo.all { it.isDigit() || it.isWhitespace() }) return null
        return limpo
    }

    fun shouldRecord(
        raw: String,
        localHits: Int,
        onlineHits: Int?,
    ): Boolean {
        if (normalize(raw) == null) return false
        if (localHits > 0) return false

        val online = onlineHits ?: return false
        return online == 0
    }
}

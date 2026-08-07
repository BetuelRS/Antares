package pt.antares.app.core.fooddata

object UsdaNameCleaner {

    private val DROP_PARTS = setOf(
        "all commercial varieties", "commercial", "nfs", "ns as to form",
        "unprepared", "includes usda commodity", "raw",
    )

    private val LEADING_ADJECTIVES = setOf(
        "white", "brown", "red", "green", "yellow", "black", "wild",
        "cooked", "boiled", "roasted", "fried", "grilled", "baked", "steamed",
        "dried", "fresh", "frozen", "canned", "smoked", "cured",
        "whole", "skim", "nonfat", "low-fat", "lowfat", "reduced-fat", "fat-free",
        "lean", "sweet", "sour", "unsweetened", "sweetened", "salted", "unsalted",
        "ripe", "young", "mature", "ground", "shredded", "sliced", "chopped",
    )

    fun clean(raw: String): String {

        val noBoiler = run {
            val i = raw.lowercase().indexOf("(includes")
            if (i >= 0) raw.substring(0, i).trim() else raw.trim()
        }
        var parts = noBoiler.split(',').map { it.trim() }.filter { it.isNotEmpty() }

        parts = parts.filter { it.lowercase() !in DROP_PARTS }
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

    private fun deshout(part: String): String =
        part.split(' ').joinToString(" ") { word ->
            val letters = word.count { it.isLetter() }
            if (letters >= 2 && word.none { it.isLetter() && it.isLowerCase() }) word.lowercase() else word
        }
}

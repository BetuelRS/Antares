package pt.antares.app.core.fooddata

import pt.antares.app.core.util.TextNormalize

object DrinkClassifier {

    private val DRINK_TERMS = listOf(

        "agua", "sumo", "sumos", "leite", "cafe", "cha", "refrigerante", "cerveja",
        "vinho", "bebida", "batido", "nectar", "limonada", "cha gelado", "iogurte liquido",
        "kefir", "kombucha", "shot", "espresso", "cappuccino", "galao", "meia de leite",

        "water", "juice", "milk", "coffee", "tea", "beer", "wine", "soda", "cola",
        "drink", "beverage", "smoothie", "latte", "lemonade", "cider", "cocktail",
        "milkshake", "shake",
    )

    private val SOLID_OVERRIDES = listOf(
        "em po", "powder", "powdered", "chocolate", "condensado", "condensed",
        "evaporado", "evaporated", "gelado", "ice cream", "sorvete", "pudim",
        "creme", "cream", "cream cheese", "queijo", "cheese", "manteiga", "butter",
        "barra", "bar", "biscoito", "cookie", "bolacha", "cereal",

        "pudding", "prepared with", "dry mix", "novelties", "frozen", "custard",
        "sauce", "molho", "gravy", "sorbet", "popsicle", "jelly", "gelatina",
        "flan", "mousse",
    )

    fun isLiquid(namePt: String, nameEn: String): Boolean {
        val hay = TextNormalize.normalize("$namePt $nameEn")
        if (hay.isBlank()) return false

        if (SOLID_OVERRIDES.any { hay.contains(it) }) return false
        return DRINK_TERMS.any { term -> containsWord(hay, term) }
    }

    private fun containsWord(haystack: String, term: String): Boolean {
        if (term.contains(' ')) return haystack.contains(term)
        var from = 0
        while (true) {
            val i = haystack.indexOf(term, from)
            if (i < 0) return false
            val before = if (i == 0) ' ' else haystack[i - 1]
            val afterIdx = i + term.length
            val after = if (afterIdx >= haystack.length) ' ' else haystack[afterIdx]
            if (!before.isLetterOrDigit() && !after.isLetterOrDigit()) return true
            from = i + 1
        }
    }
}

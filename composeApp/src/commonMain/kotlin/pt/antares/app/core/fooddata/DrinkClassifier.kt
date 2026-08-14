package pt.antares.app.core.fooddata

import pt.antares.app.core.util.TextNormalize

/**
 * Adivinha se um alimento se bebe, pelo nome. Não muda nutrição nenhuma: decide se a app
 * mostra mililitros em vez de gramas e se o registo conta para a meta de água.
 *
 * Por nome porque nenhuma das bases de alimentos declara este facto. Erra por defeito:
 * classificar um sólido como bebida é pior do que o contrário.
 */
object DrinkClassifier {

    // Nos dois idiomas do catálogo, porque um alimento pode ter nome só num deles.
    private val DRINK_TERMS = listOf(

        "agua", "sumo", "sumos", "leite", "cafe", "cha", "refrigerante", "cerveja",
        "vinho", "bebida", "batido", "nectar", "limonada", "cha gelado", "iogurte liquido",
        "kefir", "kombucha", "shot", "espresso", "cappuccino", "galao", "meia de leite",

        "water", "juice", "milk", "coffee", "tea", "beer", "wine", "soda", "cola",
        "drink", "beverage", "smoothie", "latte", "lemonade", "cider", "cocktail",
        "milkshake", "shake",
    )

    // Estas ganham sempre. Sem elas, "leite condensado", "gelado de café" e "queijo creme"
    // entravam todos na meta de água por causa de uma palavra no nome.
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

    /**
     * Palavra inteira, e não um pedaço qualquer: sem isto, "chá" aparecia dentro de
     * "chávena" e "cola" dentro de "chocolate".
     */
    private fun containsWord(haystack: String, term: String): Boolean {
        // Termos com espaço já são específicos que chegue e procuram-se tal como estão.
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

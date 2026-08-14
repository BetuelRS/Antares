package pt.antares.app.core.nutrition

data class NutrientRich(
    val foodId: String,
    val name: String,
    val kcal: Int,
    val perKcalPct: Int,
    val per100gPct: Int,
)

/**
 * Onde ir buscar um nutriente que falta. Ordena por densidade — quanto do nutriente por
 * caloria — e não por quantidade: quem tem uma lacuna precisa de a preencher sem estourar
 * o orçamento do dia.
 */
object NutrientDensity {

    // Abaixo de 5% da referência por 100 kcal não vale a pena sugerir: era preciso comer o
    // dia inteiro desse alimento para fazer diferença.
    const val MIN_PER_KCAL_PCT = 5

    // Teto por 100 g que exclui suplementos, alimentos fortificados e especiarias. São
    // densíssimos e ganhariam sempre, mas ninguém come 100 g de canela.
    const val MAX_PER_100G_PCT = 400

    /** `foods` são triplos de identificador, nome e calorias por 100 g. */
    fun rank(
        foods: List<Triple<String, String, Int>>,
        microsByFood: Map<String, Map<String, Double>>,
        key: String,
        drv: Double,
        limit: Int = 40,
    ): List<NutrientRich> {
        if (drv <= 0) return emptyList()
        return foods.mapNotNull { (id, name, kcal) ->
            if (kcal <= 0) return@mapNotNull null
            val per100g = microsByFood[id]?.get(key)?.takeIf { it > 0 } ?: return@mapNotNull null

            // Quantidade do nutriente por 100 kcal, e não por 100 g: é a medida que põe
            // espinafres e amêndoas na mesma escala.
            val perKcal = per100g * 100.0 / kcal
            val perKcalPct = (perKcal / drv * 100).toInt()
            if (perKcalPct < MIN_PER_KCAL_PCT) return@mapNotNull null
            val per100gPct = (per100g / drv * 100).toInt()
            if (per100gPct > MAX_PER_100G_PCT) return@mapNotNull null
            NutrientRich(
                foodId = id,
                name = name,
                kcal = kcal,
                perKcalPct = perKcalPct,
                per100gPct = per100gPct,
            )
        }
            // Desempate pelo nome para a lista ser sempre a mesma: uma ordem que muda entre
            // aberturas do ecrã lê-se como se os dados tivessem mudado.
            .sortedWith(compareByDescending<NutrientRich> { it.perKcalPct }.thenBy { it.name })
            .take(limit)
    }
}

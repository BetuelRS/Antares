package pt.antares.app.core.fooddata

/**
 * Traduz o nome de um alimento americano, **ou desiste e devolve nulo**.
 *
 * Desistir é a parte que interessa. A tentativa anterior traduzia palavra a palavra e
 * produzia nomes como «Pie, Dutch Maçã, Comercial» ou «Beverages, citrus fruit sumo drink,
 * congelado concentrate» — meio inglês, meio português, e piores do que qualquer dos dois.
 * Foi por isso que alguém escreveu um passo a apagá-los todos e a repor o inglês, que é
 * onde a app estava.
 *
 * A regra aqui é a oposta: **ou o nome fica inteiramente em português, ou não se lhe toca.**
 * Não há meio-termo, porque o meio-termo é o pior dos três.
 *
 * Traduz-se **segmento a segmento**, entre vírgulas, e não palavra a palavra. Os nomes do
 * USDA são listas de descritores — «Cheese, mozzarella, low sodium» — e cada descritor tem
 * uma tradução própria que raramente é a soma das palavras: «low sodium» é «baixo teor de
 * sódio», e não «baixo sódio».
 *
 * A forma com vírgulas dá ainda outra coisa de graça: as duas línguas põem o adjetivo do
 * mesmo lado. «Rice, white» dá «Arroz, branco», certo em português, sem ninguém reordenar.
 */
object UsdaNameTranslator {

    // Um nome com maiúsculas seguidas ou símbolo de marca é um produto de embalagem, e o
    // nome de uma marca não se traduz. Tentar dava «La Moderna Rikis Natas».
    private val MARCA = Regex("""\b\p{Lu}{2,}\b|®|™""")

    private val PALAVRA = Regex("""\p{L}+""")

    // O maior símbolo de unidade que a tabela usa tem duas letras: `kg`, `ml`, `oz`.
    private const val LETRAS_DE_UNIDADE = 2

    /**
     * O nome em português, ou `null` se algum segmento não estiver em [segmentos].
     *
     * [segmentos] mapeia o descritor inglês, em minúsculas, para o português. Uma medida —
     * «100 g», «2%» — passa como está: exigi-la no dicionário deitava fora o nome inteiro
     * por causa de um número.
     */
    fun traduzir(nomeEn: String, segmentos: Map<String, String>): String? {
        if (nomeEn.isBlank() || MARCA.containsMatchIn(nomeEn)) return null

        val partes = nomeEn.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (partes.isEmpty()) return null

        val traduzidas = partes.map { parte ->
            segmentos[parte.lowercase()]
                ?: parte.takeIf { eMedida(it) }
                ?: return null
        }

        return traduzidas.joinToString(", ").replaceFirstChar { it.uppercaseChar() }
    }

    /** Um número com uma unidade ao lado, e nada mais: `100 g`, `2%`, `1 oz`. */
    private fun eMedida(parte: String): Boolean =
        parte.any { it.isDigit() } &&
            PALAVRA.findAll(parte).all { it.value.length <= LETRAS_DE_UNIDADE }
}

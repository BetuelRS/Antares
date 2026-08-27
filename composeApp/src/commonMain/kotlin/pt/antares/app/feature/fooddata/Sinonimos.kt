package pt.antares.app.feature.fooddata

import pt.antares.app.core.util.TextNormalize

/**
 * As outras palavras por que a mesma comida é procurada.
 *
 * O catálogo tem **um** nome por alimento, e tem de ter: dois nomes eram dois alimentos, e
 * foi isso que o bloco D passou meses a desfazer. Mas quem procura não escreve o nome do
 * catálogo — escreve o nome que aprendeu em casa.
 *
 * Um brasileiro procura «abacaxi» e o catálogo diz «ananás». Um portista pede «cimbalino» e
 * a máquina serve um «café». Nos dois casos a app respondia que não tinha, e tem.
 *
 * **Isto vive no índice de pesquisa e em mais lado nenhum.** O nome que aparece no ecrã não
 * muda, a nutrição não muda, e nada disto é conteúdo do catálogo — é vocabulário de quem
 * escreve na caixa de procura.
 */

/**
 * Cada linha é um grupo de palavras que se procuram umas às outras.
 *
 * **Não é um mapa de errado para certo**: é uma relação simétrica. Quem escrever qualquer
 * uma encontra os alimentos nomeados com qualquer outra, e isso vale nos dois sentidos —
 * um brasileiro encontra o «ananás» e um português encontra o «abacaxi».
 *
 * A regra para entrar: as palavras têm de designar **a mesma comida**. «Bica» e «café» sim;
 * «laranja» e «tangerina» não, por muito parecidas que sejam — são frutos diferentes com
 * composições diferentes, e juntá-las era responder a uma pergunta com outra.
 */
val SINONIMOS: List<Set<String>> = listOf(
    // Brasil e Portugal, os pares que mais aparecem numa app usada dos dois lados.
    setOf("ananas", "abacaxi"),
    setOf("papaia", "mamao"),
    setOf("alperce", "damasco"),
    setOf("courgette", "abobrinha"),
    setOf("beringela", "berinjela"),
    setOf("brocolos", "brocolis"),
    setOf("bacon", "toucinho", "entremeada"),
    setOf("fiambre", "presunto cozido"),
    setOf("chavena", "xicara"),
    setOf("sumo", "suco"),
    setOf("gelado", "sorvete"),
    setOf("bolacha", "biscoito"),
    setOf("rebucado", "bala"),
    setOf("pequeno almoco", "cafe da manha"),
    setOf("frigorifico", "geladeira"),
    setOf("peru", "chester"),
    setOf("leite magro", "leite desnatado"),
    setOf("meio gordo", "semidesnatado"),
    setOf("carne picada", "carne moida"),
    setOf("grao de bico", "grao"),
    setOf("feijao verde", "vagem"),
    setOf("beterraba", "betarraba"),
    setOf("cenoura", "cenouras"),

    // O café, que em Portugal tem um nome por cidade e todos querem dizer o mesmo.
    setOf("bica", "cafe", "expresso", "espresso", "cimbalino"),
    // O galão e a meia de leite levam o mesmo, em copo e em chávena — e as duas são o que o
    // resto do mundo chama café com leite. Num grupo só, porque a composição é a mesma.
    setOf("galao", "meia de leite", "cafe com leite"),
    setOf("abatanado", "americano"),

    // Nomes de marca que passaram a nome comum. Entram porque é assim que se procuram, e
    // saem no ecrã com o nome genérico — que é o que o catálogo tem.
    setOf("nutella", "creme de avela"),
    setOf("cornflakes", "flocos de milho"),
    setOf("ketchup", "molho de tomate"),

    // O inglês das tabelas, para quem procura em português e o alimento ainda não foi
    // traduzido. São 2 909 alimentos, e este é o atalho até a tradução lá chegar.
    setOf("frango", "chicken"),
    setOf("vaca", "carne de vaca", "beef"),
    setOf("porco", "pork"),
    setOf("peixe", "fish"),
    setOf("arroz", "rice"),
    setOf("batata", "potato"),
    setOf("queijo", "cheese"),
    setOf("ovo", "egg"),
    setOf("leite", "milk"),
    setOf("pao", "bread"),
    setOf("manteiga", "butter"),
    setOf("azeite", "azeite de oliveira", "olive oil"),
    setOf("maca", "apple"),
    setOf("cru", "raw"),
    setOf("cozido", "boiled"),
    setOf("grelhado", "grilled"),
    setOf("assado", "roasted", "baked"),
    setOf("frito", "fried"),
)

/**
 * O índice invertido: de cada palavra para as outras do grupo dela.
 *
 * Constrói-se uma vez. As palavras vêm já normalizadas — sem acentos e em minúsculas —
 * porque é assim que o texto de pesquisa é guardado, e comparar uma coisa normalizada com
 * outra que não está é não encontrar nada.
 */
private val PARES: Map<String, List<String>> = buildMap {
    for (grupo in SINONIMOS) {
        val normalizado = grupo.map { TextNormalize.normalize(it) }
        for (palavra in normalizado) {
            val outras = normalizado.filter { it != palavra }
            put(palavra, (get(palavra).orEmpty() + outras).distinct())
        }
    }
}

/**
 * As palavras a acrescentar ao texto de pesquisa de um alimento.
 *
 * Procura os sinónimos **dentro** do nome já normalizado, e não só como nome inteiro: um
 * «Sumo de ananás» tem de se encontrar por «abacaxi», e o nome dele não é «ananás».
 *
 * Devolve vazio para a esmagadora maioria dos alimentos, e é o esperado — o índice só cresce
 * onde há mesmo outra palavra por que aquilo se procura.
 */
fun sinonimosDe(textoNormalizado: String): List<String> {
    if (textoNormalizado.isBlank()) return emptyList()

    val encontrados = LinkedHashSet<String>()
    for ((palavra, outras) in PARES) {
        if (contemPalavra(textoNormalizado, palavra)) encontrados += outras
    }
    // Uma palavra que já esteja no nome não se acrescenta: repeti-la no índice não muda
    // nada na procura e faz o índice crescer sem razão.
    return encontrados.filterNot { contemPalavra(textoNormalizado, it) }
}

/**
 * Se o texto contém a palavra **inteira**, e não como pedaço de outra.
 *
 * Sem isto, «pao» encontrava-se dentro de «japao» e todos os pratos japoneses ganhavam
 * «bread» no índice. É o mesmo cuidado que a composição de nomes obrigou a ter no oleoduto.
 */
private fun contemPalavra(texto: String, palavra: String): Boolean {
    var desde = 0
    while (true) {
        val i = texto.indexOf(palavra, desde)
        if (i < 0) return false
        val antes = i == 0 || !texto[i - 1].isLetterOrDigit()
        val fim = i + palavra.length
        val depois = fim == texto.length || !texto[fim].isLetterOrDigit()
        if (antes && depois) return true
        desde = i + 1
    }
}

/**
 * O texto de pesquisa completo de um alimento: o que ele diz, mais o que as pessoas
 * escrevem para o encontrar.
 */
fun textoDePesquisa(namePt: String, nameEn: String, brand: String?): String {
    val base = TextNormalize.normalize("$namePt $nameEn ${brand.orEmpty()}")
    val extra = sinonimosDe(base)
    return if (extra.isEmpty()) base else "$base ${extra.joinToString(" ")}"
}

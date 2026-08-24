package pt.antares.app.core.confecao

import kotlinx.serialization.Serializable

/** Um método de confeção, como se lê no ecrã. */
@Serializable
data class MetodoDeConfecao(val id: String, val nome: String, val nomeEn: String)

/**
 * O que acontece a uma família de alimento quando lhe aplicam um método.
 *
 * O [rendimento] é a fracção do peso que sobra — 0,77 quer dizer que 100 g de vaca crua dão
 * 77 g de vaca grelhada. É nulo quando ninguém o mediu para esta combinação, e nesse caso
 * **pergunta-se o peso a quem cozinhou** em vez de se inventar um número. As tabelas do USDA
 * só publicam rendimentos para carne e aves.
 *
 * As [retencoes] são a fracção de cada nutriente que sobrevive, por chave do vocabulário da
 * app. Um nutriente que não esteja aqui **não tem factor publicado**, e fica como está: o
 * selénio, o iodo e as vitaminas D, E e K não aparecem na tabela de retenção, e escrever-lhes
 * um factor era inventar.
 */
@Serializable
data class LinhaDeConfecao(
    val familia: String,
    val metodo: String,
    val rendimento: Double? = null,
    val rendimentoDeOutraCarne: Boolean = false,
    val preparacoes: Int = 0,
    val comMolho: Boolean = false,
    val retencoes: Map<String, Double> = emptyMap(),
)

/** A tabela inteira, como o oleoduto a escreve. */
@Serializable
data class TabelaDeConfecao(
    val versao: Int,
    val metodos: List<MetodoDeConfecao> = emptyList(),
    val linhas: List<LinhaDeConfecao> = emptyList(),
) {
    private val porChave: Map<String, LinhaDeConfecao> by lazy {
        linhas.associateBy { "${it.familia}:${it.metodo}" }
    }

    fun linha(familia: String?, metodo: String): LinhaDeConfecao? =
        familia?.let { porChave["$it:$metodo"] }

    /** Os métodos que fazem sentido para esta família, pela ordem em que a tabela os declara. */
    fun metodosDe(familia: String?): List<MetodoDeConfecao> {
        if (familia == null) return emptyList()
        val disponiveis = linhas.filter { it.familia == familia }.map { it.metodo }.toSet()
        return metodos.filter { it.id in disponiveis }
    }

    companion object {
        const val FICHEIRO = "files/confecao.json"

        /** Uma tabela que não abriu. A app funciona na mesma — apenas não oferece confeção. */
        val VAZIA = TabelaDeConfecao(versao = 0)
    }
}

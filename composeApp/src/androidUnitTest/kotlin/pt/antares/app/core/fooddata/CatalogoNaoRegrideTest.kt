package pt.antares.app.core.fooddata

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pt.antares.app.feature.fooddata.Catalogo
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Os nomes que foram corrigidos à mão não voltam ao nome de laboratório.
 *
 * Durante meses, cinco passos do semeador antigo arrumaram nomes americanos — «Beef,
 * ground, 80% lean» virou «Ground beef, 80% lean», e o que o dicionário cobria por inteiro
 * passou a português. Essas correções viviam em código que corria no telemóvel. Quando o
 * catálogo passou a ser construído fora da app, **reconstruí-lo das fontes devolvia-lhes o
 * nome de origem**, e nada os tornaria a limpar.
 *
 * Por isso foram extraídos para `tools/catalogo/correcoes.json`, e é isso que este teste
 * guarda: o que estava corrigido continua corrigido. Uma reconstrução que esqueça o passo
 * de aplicar as correções passa em todos os outros testes — tem os alimentos todos, na
 * ordem certa, com a nutrição certa. Só os nomes é que voltam atrás.
 */
class CatalogoNaoRegrideTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val catalogo: Catalogo by lazy {
        json.decodeFromString(File("src/commonMain/composeResources/files/catalogo.json").readText())
    }
    private val correcoes: JsonObject by lazy {
        json.parseToJsonElement(File("../tools/catalogo/correcoes.json").readText()).jsonObject
    }
    private val desvios: JsonObject by lazy {
        json.parseToJsonElement(File("../tools/catalogo/desvios.json").readText()).jsonObject
    }

    @Test
    fun `os nomes corrigidos estao todos no catalogo`() {
        val nomes = correcoes["nomes"]!!.jsonObject
        assertTrue(nomes.size > MINIMO_DE_CORRECOES, "só ${nomes.size} nomes corrigidos — a leitura partiu-se")

        val porId = catalogo.alimentos.associateBy { it.id }
        val regressoes = nomes.entries
            .mapNotNull { (id, nome) ->
                val alimento = porId[id] ?: return@mapNotNull null
                val esperado = nome.jsonPrimitive.content

                /**
                 * Uma «correção» igual ao nome da fonte não é decisão de ninguém.
                 *
                 * O `correcoes.json` foi extraído de um telemóvel onde os dezoito passos
                 * tinham corrido: guarda o nome que lá estava, e para 1 380 alimentos esse
                 * nome era o inglês da tabela, por nunca ter sido tocado. Exigir que ele
                 * sobreviva é exigir que o catálogo **fique** em inglês — e foi isso que
                 * este teste passou a fazer quando o vocabulário começou a traduzir.
                 *
                 * O que ele existe para apanhar continua a valer: um nome que **alguém
                 * escreveu** não pode voltar ao da origem.
                 */
                if (esperado == alimento.nameEn) return@mapNotNull null

                if (alimento.namePt == esperado) null else "$id: esperado «$esperado», veio «${alimento.namePt}»"
            }

        assertTrue(
            regressoes.isEmpty(),
            "${regressoes.size} nomes voltaram ao nome de origem:\n" + regressoes.take(10).joinToString("\n"),
        )
    }

    @Test
    fun `o que foi podado nao volta`() {

        // As três podas do semeador antigo apagaram duplicados depois de alguém decidir
        // qual dos dois ficava. Reconstruir sem essa lista ressuscitava-os, e a pesquisa
        // voltava a dar o mesmo alimento duas vezes com nomes ligeiramente diferentes.
        val podados = correcoes["podados"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet()
        val ressuscitados = catalogo.alimentos.map { it.id }.filter { it in podados }

        assertTrue(ressuscitados.isEmpty(), "voltaram ao catálogo: ${ressuscitados.take(10)}")
    }

    @Test
    fun `os desvios declarados estao mesmo de fora`() {

        // Uma lista de desvios que contenha um alimento que afinal entrou é pior do que
        // não haver lista: quem a lê fica a acreditar que ele falta.
        val declarados = desvios["desvios"]!!.jsonObject.keys
        val total = desvios["total"]!!.jsonPrimitive.content.toInt()
        assertEquals(total, declarados.size, "o total escrito no ficheiro não bate com a lista")

        val presentes = catalogo.alimentos.map { it.id }.filter { it in declarados }
        assertTrue(presentes.isEmpty(), "declarados como fora, mas estão dentro: ${presentes.take(10)}")
    }

    @Test
    fun `cada desvio tem uma razao escrita`() {
        val semRazao = desvios["desvios"]!!.jsonObject.entries
            .filter { (_, razao) -> razao.jsonPrimitive.content.length < TAMANHO_DE_UMA_RAZAO }
            .map { it.key }

        assertTrue(semRazao.isEmpty(), "desvios sem razão a sério: ${semRazao.take(10)}")
    }

    private companion object {
        const val MINIMO_DE_CORRECOES = 2000
        const val TAMANHO_DE_UMA_RAZAO = 30
    }
}

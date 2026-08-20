package pt.antares.app.feature.fooddata

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.antares.app.core.database.daos.FoodDao
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.fooddata.Barcode
import pt.antares.app.core.network.off.OffApi
import pt.antares.app.core.network.off.OffMapper
import pt.antares.app.core.util.FtsQuery
import pt.antares.app.core.util.TextNormalize

sealed interface OffFetch {
    data class Found(val food: FoodEntity) : OffFetch
    data object NotFound : OffFetch
    data object NetworkError : OffFetch

    /** A pessoa desligou a pesquisa em linha. Não é falha de rede nem produto inexistente. */
    data object Desligada : OffFetch
}

/**
 * O resultado de uma procura na Open Food Facts. Três estados e não uma lista anulável: com
 * `null` a valer «sem rede», desligar a pesquisa passava a ler-se no ecrã como uma falha de
 * ligação — a app a culpar a rede de uma escolha da pessoa.
 */
sealed interface OffSearch {
    data class Resultados(val foods: List<FoodEntity>) : OffSearch
    data object SemRede : OffSearch
    data object Desligada : OffSearch
}

/**
 * A única porta da app para a Open Food Facts.
 *
 * As duas funções públicas começam pela mesma pergunta, e é por isso que o interruptor vive
 * aqui e não nos ecrãs: um ecrã novo que se esqueça de o consultar não existe — não há por
 * onde chegar à rede sem passar por estas duas.
 */
class OffRepository(
    private val api: OffApi,
    private val foodDao: FoodDao,
    private val io: CoroutineDispatcher,

    // Uma função e não as preferências inteiras: mantém este repositório sem saber nada de
    // DataStore, e deixa um teste ligá-la e desligá-la com uma linha.
    private val emLinha: suspend () -> Boolean = { true },
) {
    private fun now() = Clock.System.now().toEpochMilliseconds()

    /**
     * Procura o produto na Open Food Facts e guarda-o. Tenta cada variante do código: o
     * mesmo produto está registado lá com ou sem o zero à frente conforme quem o inseriu.
     */
    suspend fun fetchAndCache(barcode: String): OffFetch = withContext(io) {
        if (!emLinha()) return@withContext OffFetch.Desligada


        // A distinção entre falha de rede e produto inexistente é o que o ecrã precisa: um
        // convida a tentar outra vez, o outro a criar o alimento à mão. A bandeira sobrevive
        // ao ciclo porque uma variante pode falhar por rede e a seguinte por não existir.
        var networkFailed = false
        for (code in Barcode.searchVariants(barcode)) {
            val response = runCatching { api.product(code) }.getOrNull()
            if (response == null) {
                networkFailed = true
                continue
            }
            val product = response.product
            if (response.status == 1 && product != null) {
                val food = OffMapper.toFood(product, Barcode.normalize(code) ?: code, now())
                cache(food)
                return@withContext OffFetch.Found(food)
            }
        }
        if (networkFailed) OffFetch.NetworkError else OffFetch.NotFound
    }

    /**
     * Pesquisa por texto na Open Food Facts. Null distingue-se de lista vazia: null é
     * falha de rede, vazio é não haver resultados — e o ecrã diz coisas diferentes.
     *
     * Os três filtros a seguir existem porque a base é preenchida por voluntários e devolve
     * muita coisa inútil para quem quer registar comida.
     */
    suspend fun procurar(query: String): OffSearch = withContext(io) {
        if (!emLinha()) return@withContext OffSearch.Desligada
        when (val encontrados = searchOnline(query)) {
            null -> OffSearch.SemRede
            else -> OffSearch.Resultados(encontrados)
        }
    }

    // O caminho de rede puro, sem o interruptor. Interno para os testes o poderem exercitar
    // sozinho; fora deles chama-se sempre o `procurar`, e o `InterruptorDaPesquisaTest` guarda-o.
    internal suspend fun searchOnline(query: String): List<FoodEntity>? = withContext(io) {

        val response = runCatching { api.search(query) }.getOrNull() ?: return@withContext null
        val tokens = FtsQuery.tokens(query)

        response.products.asSequence()
            .mapNotNull { p ->
                val code = p.code?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

                // Sem nome, o [OffMapper] usaria o código de barras como nome: aceitável ao
                // ler um código concreto, inútil numa lista de resultados.
                val hasRealName = !p.productNamePt.isNullOrBlank() || !p.productName.isNullOrBlank()
                if (!hasRealName) return@mapNotNull null
                OffMapper.toFood(p, code, now())
            }

            // Produto sem nenhum macro é uma ficha por preencher, não um alimento.
            .filter { it.kcal > 0 || it.proteinG > 0 || it.carbsG > 0 || it.fatG > 0 }

            // A pesquisa deles é generosa e devolve coisas sem relação com o que se pediu;
            // exigir pelo menos uma palavra no nome ou na marca corta o pior.
            .filter { food ->
                if (tokens.isEmpty()) return@filter true
                val name = TextNormalize.normalize("${food.namePt} ${food.brand.orEmpty()}")
                tokens.any { name.contains(it) }
            }
            .distinctBy { it.id }
            .toList()
    }

    private suspend fun cache(food: FoodEntity) {
        foodDao.upsertWithFts(
            food,
            TextNormalize.normalize("${food.namePt} ${food.nameEn} ${food.brand.orEmpty()}"),
        )
    }
}

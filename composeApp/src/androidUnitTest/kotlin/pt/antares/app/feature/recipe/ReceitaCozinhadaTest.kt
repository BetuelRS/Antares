package pt.antares.app.feature.recipe

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.confecao.LeitorDeConfecao
import pt.antares.app.core.confecao.LinhaDeConfecao
import pt.antares.app.core.confecao.MetodoDeConfecao
import pt.antares.app.core.confecao.TabelaDeConfecao
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.RecipeEntity
import pt.antares.app.core.database.entities.RecipeIngredientEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.nutrition.microsDeJson
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A receita cozinhada, do princípio ao fim: a família do alimento, a linha da tabela, a
 * retenção aplicada por ingrediente.
 *
 * O `RetencaoNaReceitaTest` cobra a aritmética sozinha. O que se pode partir aqui é a
 * ligação — a família guardada no alimento, a linha procurada na tabela, o método gravado na
 * receita — e isso não se vê em nenhum dos dois lados separados.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReceitaCozinhadaTest : ViewModelHarness() {

    private val chaveVitC = "vitC_mg"

    /**
     * Uma tabela mínima com o que estes testes precisam: cozer legumes guarda 75 % da
     * vitamina C e não tem rendimento publicado; grelhar vaca guarda 80 % e deixa 77 % do
     * peso. São os números reais das duas tabelas do USDA, para o teste falhar se alguém os
     * trocar por outros.
     */
    private val tabela = TabelaDeConfecao(
        versao = 1,
        metodos = listOf(
            MetodoDeConfecao("cozido", "Cozido", "Boiled"),
            MetodoDeConfecao("grelhado", "Grelhado", "Grilled"),
        ),
        linhas = listOf(
            LinhaDeConfecao(
                familia = "legumes",
                metodo = "cozido",
                rendimento = null,
                retencoes = mapOf(chaveVitC to 0.75),
            ),
            LinhaDeConfecao(
                familia = "vaca",
                metodo = "grelhado",
                rendimento = 0.77,
                retencoes = mapOf(chaveVitC to 0.8),
            ),
        ),
    )

    private fun repository() = RecipeRepository(
        db.recipeDao(),
        db.recipeIngredientDao(),
        db.recipeStepDao(),
        db.foodDao(),
        diaryRepository(),
        LeitorDeConfecao(dispatcher, precarregada = tabela),
        dispatcher,
    )

    private suspend fun alimento(id: String, familia: String?, vitC: Double) {
        db.foodDao().upsert(
            FoodEntity(
                id = id,
                source = FoodSource.SEED,
                sourceRef = null,
                namePt = id,
                nameEn = id,
                brand = null,
                kcal = 23,
                proteinG = 2.9,
                carbsG = 3.6,
                sugarsG = null,
                fatG = 0.4,
                satFatG = null,
                servingName = null,
                servingGrams = null,
                familia = familia,
                microsJson = """{"$chaveVitC":$vitC}""",
                updatedAt = 0L,
            ),
        )
    }

    private suspend fun receita(
        id: String,
        metodo: String?,
        pesoFinal: Double?,
        ingredientes: List<Pair<String, Double>>,
    ) {
        db.recipeDao().upsert(
            RecipeEntity(
                id = id,
                name = id,
                yieldGrams = pesoFinal,
                metodo = metodo,
                updatedAt = 0L,
            ),
        )
        for ((foodId, gramas) in ingredientes) {
            db.recipeIngredientDao().upsert(
                RecipeIngredientEntity(
                    id = "$id-$foodId",
                    recipeId = id,
                    foodId = foodId,
                    grams = gramas,
                    updatedAt = 0L,
                ),
            )
        }
    }

    @Test
    fun `sem metodo a receita nao perde nada`() = runTest(dispatcher) {
        alimento("espinafres", familia = "legumes", vitC = 28.0)
        receita("sopa", metodo = null, pesoFinal = 400.0, listOf("espinafres" to 500.0))

        val n = repository().nutrition("sopa")

        // 140 mg em 400 g. É o número antigo, e continua a sair sem método escolhido.
        assertEquals(35.0, n.microsPer100[chaveVitC]!!, 1e-9)
    }

    @Test
    fun `com metodo a receita perde o que a tabela diz`() = runTest(dispatcher) {
        alimento("espinafres", familia = "legumes", vitC = 28.0)
        receita("sopa", metodo = "cozido", pesoFinal = 400.0, listOf("espinafres" to 500.0))

        val n = repository().nutrition("sopa")

        assertEquals(26.25, n.microsPer100[chaveVitC]!!, 1e-9)
        assertTrue(
            n.microsPer100[chaveVitC]!! < 28.0,
            "cozer não pode deixar 100 g com mais vitamina C do que tinham em cru",
        )
    }

    @Test
    fun `um ingrediente sem familia nao perde nada`() = runTest(dispatcher) {
        alimento("espinafres", familia = "legumes", vitC = 28.0)
        alimento("azeite", familia = null, vitC = 10.0)
        receita(
            "refogado",
            metodo = "cozido",
            pesoFinal = 200.0,
            listOf("espinafres" to 100.0, "azeite" to 100.0),
        )

        val n = repository().nutrition("refogado")

        // 28 × 0,75 = 21 mg dos espinafres, mais os 10 inteiros do azeite, em 200 g.
        assertEquals(15.5, n.microsPer100[chaveVitC]!!, 1e-9)
    }

    @Test
    fun `os metodos oferecidos sao os que alguma familia conhece`() = runTest(dispatcher) {
        alimento("espinafres", familia = "legumes", vitC = 28.0)
        alimento("bife", familia = "vaca", vitC = 0.0)
        receita("cozido", metodo = null, pesoFinal = null, listOf("espinafres" to 100.0, "bife" to 200.0))

        val metodos = repository().metodosPara("cozido").map { it.id }

        // A união, e não a intersecção: as tabelas não publicam rendimento de cozer legumes,
        // e a intersecção deixava um cozido de carne com legumes sem «cozido».
        assertEquals(listOf("cozido", "grelhado"), metodos)
    }

    @Test
    fun `uma receita sem familia nenhuma nao oferece metodos`() = runTest(dispatcher) {
        alimento("azeite", familia = null, vitC = 0.0)
        receita("molho", metodo = null, pesoFinal = null, listOf("azeite" to 100.0))

        assertTrue(repository().metodosPara("molho").isEmpty())
    }

    @Test
    fun `o peso sugerido sai dos rendimentos publicados`() = runTest(dispatcher) {
        alimento("bife", familia = "vaca", vitC = 0.0)
        receita("grelhada", metodo = "grelhado", pesoFinal = null, listOf("bife" to 500.0))

        assertEquals(385.0, repository().pesoFinalSugerido("grelhada", "grelhado")!!, 1e-9)
    }

    /**
     * Sem rendimento publicado para a maior parte do peso, não há sugestão.
     *
     * Um número que descrevesse um quinto do tacho e passasse por descrever o tacho todo era
     * pior do que campo vazio: o campo vazio pede que se pese, e o número não.
     */
    @Test
    fun `sem rendimento na maior parte do peso nao ha sugestao`() = runTest(dispatcher) {
        alimento("espinafres", familia = "legumes", vitC = 28.0)
        alimento("bife", familia = "vaca", vitC = 0.0)
        receita("mistura", metodo = "grelhado", pesoFinal = null, listOf("espinafres" to 800.0, "bife" to 200.0))

        assertNull(repository().pesoFinalSugerido("mistura", "grelhado"))
    }

    @Test
    fun `sem metodo escolhido nao ha peso sugerido`() = runTest(dispatcher) {
        alimento("bife", familia = "vaca", vitC = 0.0)
        receita("grelhada", metodo = null, pesoFinal = null, listOf("bife" to 500.0))

        assertNull(repository().pesoFinalSugerido("grelhada", null))
    }

    @Test
    fun `escolher o metodo grava-o na receita`() = runTest(dispatcher) {
        alimento("bife", familia = "vaca", vitC = 0.0)
        receita("grelhada", metodo = null, pesoFinal = null, listOf("bife" to 500.0))

        val repo = repository()
        repo.updateMetodo("grelhada", "grelhado")
        assertEquals("grelhado", repo.recipeById("grelhada")?.metodo)

        repo.updateMetodo("grelhada", null)
        assertNull(repo.recipeById("grelhada")?.metodo)
    }

    /**
     * O que fica no diário é o que esteve no ecrã.
     *
     * A receita é viva e o registo é congelado — e a cópia congelada tem de ser a cozinhada,
     * não a crua. Sem isto, escolher o método mudava o número que se via e não o que se
     * guardava, que é a pior das duas coisas.
     */
    @Test
    fun `o registo no diario leva a nutricao cozinhada`() = runTest(dispatcher) {
        alimento("espinafres", familia = "legumes", vitC = 28.0)
        receita("sopa", metodo = "cozido", pesoFinal = 400.0, listOf("espinafres" to 500.0))

        repository().logRecipe(
            "sopa",
            grams = 400.0,
            slot = MealSlot.LUNCH,
            epochDay = 20_000L,
        )

        val registos = db.foodLogDao().dayLogs(20_000L)
        assertEquals(1, registos.size)
        val micros = microsDeJson(registos.first().microsPer100Json)
        assertEquals(26.25, micros[chaveVitC]!!, 1e-9)
    }
}

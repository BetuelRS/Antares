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
import pt.antares.app.core.database.entities.RecipeIngredientEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O aviso do peso final, que até à 2.18.0 só falava com o campo vazio.
 *
 * A 2.8.0 trouxe a previsão — e ela só aparecia como sugestão ao lado de um campo por
 * preencher. Escrever 2000 g numa receita de 400 g de ingredientes não dizia nada, e todos
 * os valores por 100 g saíam cinco vezes errados em silêncio.
 *
 * **O limiar não é um número escolhido por mim.** Medido no ficheiro de confeção: dentro da
 * mesma família o rendimento muda até 0,43 só por se escolher outro método — o porco vai de
 * 0,39 estufado a 0,82 assado. Uma percentagem fixa estaria errada para metade das receitas.
 * O que a app compara é com o intervalo que **os próprios métodos publicados** dão.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PesoFinalForaDoPrevistoTest : ViewModelHarness() {

    /**
     * Os números reais do porco no ficheiro de confeção — o par mais afastado que a tabela
     * tem. Se alguém os trocar por outros, este teste conta a história errada e falha.
     */
    private val tabela = TabelaDeConfecao(
        versao = 1,
        metodos = listOf(
            MetodoDeConfecao("estufado", "Estufado", "Braised"),
            MetodoDeConfecao("assado", "Assado", "Roasted"),
        ),
        linhas = listOf(
            LinhaDeConfecao(familia = "porco", metodo = "estufado", rendimento = 0.39),
            LinhaDeConfecao(familia = "porco", metodo = "assado", rendimento = 0.82),
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

    private suspend fun alimento(id: String, familia: String?) {
        db.foodDao().upsert(
            FoodEntity(
                id = id,
                source = FoodSource.SEED,
                sourceRef = null,
                namePt = id,
                nameEn = id,
                brand = null,
                kcal = 200,
                proteinG = 20.0,
                carbsG = 0.0,
                sugarsG = null,
                fatG = 12.0,
                satFatG = null,
                microsJson = null,
                servingName = null,
                servingGrams = null,
                familia = familia,
                updatedAt = 1L,
            ),
        )
    }

    private suspend fun receitaDePorco(gramas: Double = 1000.0): String {
        val repo = repository()
        alimento("lombo", "porco")
        val id = repo.createRecipe("Lombo assado", yieldGrams = null)
        db.recipeIngredientDao().upsert(
            RecipeIngredientEntity(
                id = "i1",
                recipeId = id,
                foodId = "lombo",
                grams = gramas,
                updatedAt = 1L,
            ),
        )
        return id
    }

    @Test
    fun `o envelope vai do metodo que mais encolhe ao que menos encolhe`() = runTest {
        val id = receitaDePorco(1000.0)

        val envelope = assertNotNull(repository().envelopeDePesoFinal(id))
        assertEquals(390.0, envelope.start, 0.001, "o estufado é o que mais encolhe")
        assertEquals(820.0, envelope.endInclusive, 0.001, "o assado é o que menos encolhe")
    }

    /**
     * Um peso escrito que cabe no envelope não é discutido.
     *
     * 500 g estão entre os 390 do estufado e os 820 do assado — quem cozinhou pode ter feito
     * qualquer coisa pelo meio, e a app não tem opinião sobre isso.
     */
    @Test
    fun `um peso que algum metodo explica nao leva aviso`() = runTest {
        val id = receitaDePorco(1000.0)
        val envelope = assertNotNull(repository().envelopeDePesoFinal(id))

        assertTrue(500.0 in envelope)
        assertTrue(390.0 in envelope, "a fronteira de baixo conta como explicada")
        assertTrue(820.0 in envelope, "a de cima também")
    }

    /** E 2000 g não. Nenhum modo de cozinhar um quilo de porco dá dois quilos de porco. */
    @Test
    fun `um peso que nenhum metodo explica fica de fora`() = runTest {
        val id = receitaDePorco(1000.0)
        val envelope = assertNotNull(repository().envelopeDePesoFinal(id))

        assertTrue(2000.0 !in envelope)
        assertTrue(100.0 !in envelope, "encolher para um décimo também não se explica")
    }

    /**
     * Sem cobertura não há envelope, e sem envelope a app não avisa de nada.
     *
     * Uma salada não tem família de confeção nenhuma. Avisar aí era inventar uma opinião a
     * partir de uma tabela que não fala do assunto.
     */
    @Test
    fun `uma receita sem familia conhecida nao tem envelope`() = runTest {
        val repo = repository()
        alimento("alface", null)
        val id = repo.createRecipe("Salada", yieldGrams = null)
        db.recipeIngredientDao().upsert(
            RecipeIngredientEntity(id = "i1", recipeId = id, foodId = "alface", grams = 300.0, updatedAt = 1L),
        )

        assertNull(repo.envelopeDePesoFinal(id))
    }
}

package pt.antares.app.feature.recipe

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.confecao.LeitorDeConfecao
import pt.antares.app.core.confecao.TabelaDeConfecao
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Os passos de preparação.
 *
 * **A ordem é o dado**, e é isso que estes testes defendem. Num ingrediente a ordem não
 * significa nada — 200 g de arroz e 150 g de frango são a mesma receita seja qual for a
 * ordem por que se escrevam. Num passo, «leva ao lume» antes de «tempera» é outra receita.
 *
 * É por isso que há uma coluna `posicao` e uma renumeração, em vez da ordem de introdução
 * que os ingredientes usam.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PassosDaReceitaTest : ViewModelHarness() {

    private fun repository() = RecipeRepository(
        db.recipeDao(),
        db.recipeIngredientDao(),
        db.recipeStepDao(),
        db.foodDao(),
        diaryRepository(),
        LeitorDeConfecao(dispatcher, precarregada = TabelaDeConfecao(versao = 1)),
        dispatcher,
    )

    private suspend fun receitaComTresPassos(): Pair<RecipeRepository, String> {
        val repo = repository()
        val id = repo.createRecipe("Arroz de forno", yieldGrams = null)
        repo.addStep(id, "Refogar a cebola")
        repo.addStep(id, "Juntar o arroz")
        repo.addStep(id, "Levar ao forno 20 minutos")
        return repo to id
    }

    private suspend fun textos(repo: RecipeRepository, id: String) =
        repo.observeSteps(id).first().map { it.texto }

    // ---- escrever -------------------------------------------------------------------

    @Test
    fun `os passos saem pela ordem em que se escreveram`() = runTest {
        val (repo, id) = receitaComTresPassos()

        assertEquals(
            listOf("Refogar a cebola", "Juntar o arroz", "Levar ao forno 20 minutos"),
            textos(repo, id),
        )
        assertEquals(listOf(0, 1, 2), repo.observeSteps(id).first().map { it.posicao })
    }

    /** Um passo em branco não instrui nada, e ocupava uma linha numerada. */
    @Test
    fun `um passo em branco nao entra`() = runTest {
        val repo = repository()
        val id = repo.createRecipe("Salada", yieldGrams = null)

        assertNull(repo.addStep(id, "   "))
        assertTrue(textos(repo, id).isEmpty())
    }

    @Test
    fun `corrigir um passo nao lhe mexe na posicao`() = runTest {
        val (repo, id) = receitaComTresPassos()
        val meio = repo.observeSteps(id).first()[1]

        repo.updateStep(meio, "Juntar o arroz e o caldo")

        assertEquals(
            listOf("Refogar a cebola", "Juntar o arroz e o caldo", "Levar ao forno 20 minutos"),
            textos(repo, id),
        )
    }

    // ---- mover ----------------------------------------------------------------------

    @Test
    fun `subir um passo troca-o com o de cima`() = runTest {
        val (repo, id) = receitaComTresPassos()

        repo.moveStep(id, from = 2, to = 1)

        assertEquals(
            listOf("Refogar a cebola", "Levar ao forno 20 minutos", "Juntar o arroz"),
            textos(repo, id),
        )
    }

    /**
     * Mover do fim para o princípio empurra os outros, e não troca dois.
     *
     * É a diferença entre mover e trocar, e nota-se com três: mover o terceiro para o
     * primeiro tem de dar 3-1-2, e não 3-2-1.
     */
    @Test
    fun `mover empurra os do meio em vez de trocar dois`() = runTest {
        val (repo, id) = receitaComTresPassos()

        repo.moveStep(id, from = 2, to = 0)

        assertEquals(
            listOf("Levar ao forno 20 minutos", "Refogar a cebola", "Juntar o arroz"),
            textos(repo, id),
        )
        assertEquals(listOf(0, 1, 2), repo.observeSteps(id).first().map { it.posicao })
    }

    /** Fora da lista não faz nada — não é engano, é o fim da lista. */
    @Test
    fun `mover para fora da lista nao mexe em nada`() = runTest {
        val (repo, id) = receitaComTresPassos()

        repo.moveStep(id, from = 0, to = -1)
        repo.moveStep(id, from = 0, to = 9)
        repo.moveStep(id, from = 1, to = 1)

        assertEquals(
            listOf("Refogar a cebola", "Juntar o arroz", "Levar ao forno 20 minutos"),
            textos(repo, id),
        )
    }

    // ---- apagar e devolver ----------------------------------------------------------

    /**
     * Apagar renumera o que sobra.
     *
     * Sem isto ficava um buraco na sequência — 0, 2 — e o passo seguinte a entrar herdava a
     * posição de um que ainda lá está, ficando dois passos empatados na mesma posição.
     */
    @Test
    fun `apagar fecha o buraco na numeracao`() = runTest {
        val (repo, id) = receitaComTresPassos()
        val meio = repo.observeSteps(id).first()[1]

        repo.removeStep(meio)

        val sobra = repo.observeSteps(id).first()
        assertEquals(listOf("Refogar a cebola", "Levar ao forno 20 minutos"), sobra.map { it.texto })
        assertEquals(listOf(0, 1), sobra.map { it.posicao })
    }

    @Test
    fun `um passo novo entra depois de apagar, sem empatar posicoes`() = runTest {
        val (repo, id) = receitaComTresPassos()
        repo.removeStep(repo.observeSteps(id).first()[1])

        repo.addStep(id, "Deixar repousar")

        assertEquals(listOf(0, 1, 2), repo.observeSteps(id).first().map { it.posicao })
    }

    /** Desfazer devolve o passo à lista, e a numeração volta a fechar-se sobre ele. */
    @Test
    fun `devolver um passo apagado repoe a lista inteira`() = runTest {
        val (repo, id) = receitaComTresPassos()
        val meio = repo.observeSteps(id).first()[1]
        repo.removeStep(meio)

        repo.restoreStep(meio.id, id)

        val voltou = repo.observeSteps(id).first()
        assertEquals(3, voltou.size)
        assertEquals(listOf(0, 1, 2), voltou.map { it.posicao })
        assertTrue("Juntar o arroz" in voltou.map { it.texto })
    }

    // ---- os passos não entram em conta nenhuma --------------------------------------

    /**
     * Escrever passos não muda um número da receita.
     *
     * É o que os mantém honestos: uma receita continua a ser a soma dos ingredientes, e os
     * passos são texto de quem cozinhou para si próprio. Se um dia entrarem numa conta, é
     * aqui que se vê.
     */
    @Test
    fun `os passos nao mexem na nutricao`() = runTest {
        val repo = repository()
        val id = repo.createRecipe("Arroz", yieldGrams = null)
        val antes = repo.nutrition(id)

        repo.addStep(id, "Levar ao lume")

        assertEquals(antes, repo.nutrition(id))
    }
}

package pt.antares.app.feature.fooddata

import pt.antares.app.core.calc.RecipeNutrition
import pt.antares.app.core.database.entities.MealTemplateEntity
import pt.antares.app.core.database.entities.RecipeEntity
import pt.antares.app.core.model.MealSlot
import pt.antares.app.feature.recipe.RecipeSummary
import pt.antares.app.feature.templates.ModeloComResumo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A lista só, que é o ponto principal do esboço da área 05.
 *
 * A app tinha duas listas para a mesma pergunta — «o que é que eu já montei?» —, e a 2.18.0
 * juntou-as num separador mas deixou-as em duas secções com dois títulos. Duas secções
 * continuam a obrigar a saber a diferença antes de procurar, e a diferença é como a
 * refeição foi construída, que não é a pergunta de quem a quer registar.
 *
 * O que estes testes guardam é que a junção é **mesmo** uma lista: sem agrupar por origem,
 * e sem uma origem a passar à frente da outra.
 */
class RefeicaoGuardadaTest {

    private fun modelo(nome: String) = ModeloComResumo(
        modelo = MealTemplateEntity(id = nome, name = nome, slot = MealSlot.LUNCH, updatedAt = 0),
        itens = 4,
        kcal = 640,
    )

    private fun receita(nome: String) = RecipeSummary(
        recipe = RecipeEntity(id = nome, name = nome, yieldGrams = null, updatedAt = 0),
        nutrition = RecipeNutrition(
            totalKcal = 512,
            totalProteinG = 30.0,
            totalCarbsG = 40.0,
            totalFatG = 20.0,
            basisGrams = 1000.0,
        ),
        ingredientCount = 7,
    )

    @Test
    fun `as duas origens ficam na mesma lista, por ordem de nome`() {
        val lista = juntarRefeicoes(
            modelos = listOf(modelo("Almoço de sempre"), modelo("Batido pós-treino")),
            receitas = listOf(receita("Bacalhau à Gomes de Sá")),
        )

        assertEquals(
            listOf("Almoço de sempre", "Bacalhau à Gomes de Sá", "Batido pós-treino"),
            lista.map { it.nome },
        )
    }

    /**
     * O que separa uma lista só de duas secções disfarçadas: uma receita **entre** duas
     * refeições guardadas. Se a ordenação fosse por origem, ela iria para uma ponta.
     */
    @Test
    fun `a origem nao decide a posicao`() {
        val lista = juntarRefeicoes(
            modelos = listOf(modelo("Almoço de sempre"), modelo("Batido pós-treino")),
            receitas = listOf(receita("Bacalhau à Gomes de Sá")),
        )

        assertTrue(lista[0] is RefeicaoGuardada.DoDiario)
        assertTrue(lista[1] is RefeicaoGuardada.DeIngredientes)
        assertTrue(lista[2] is RefeicaoGuardada.DoDiario)
    }

    /**
     * «almoço» e «Almoço» em pontas opostas seria o alfabeto a mandar mais do que a leitura.
     *
     * Os nomes são escolhidos para **separar** as duas ordenações: por código de carácter,
     * `Banana` (B, 66) vem antes de `abacate` (a, 97) e a lista sai ao contrário do
     * alfabeto. Com nomes todos capitalizados as duas ordens coincidem e o teste passaria
     * na mesma com a comparação errada — foi o que aconteceu à primeira versão dele.
     */
    @Test
    fun `a ordem ignora maiusculas`() {
        val lista = juntarRefeicoes(
            modelos = listOf(modelo("Banana"), modelo("abacate")),
            receitas = listOf(receita("Caldo verde")),
        )

        assertEquals(listOf("abacate", "Banana", "Caldo verde"), lista.map { it.nome })
    }

    /**
     * As duas tabelas têm identificadores próprios e podem colidir. A chave da lista tem de
     * os separar, senão duas linhas diferentes partilham a chave e o Compose reaproveita a
     * errada ao rolar.
     */
    @Test
    fun `a chave distingue as origens com o mesmo identificador`() {
        val lista = juntarRefeicoes(listOf(modelo("x")), listOf(receita("x")))

        assertEquals(2, lista.map { it.chave }.toSet().size)
    }

    @Test
    fun `sem nada, a lista fica vazia`() {
        assertEquals(emptyList(), juntarRefeicoes(emptyList(), emptyList()))
    }

    /**
     * Uma receita sem nome é possível, e apareceu no aparelho.
     *
     * A linha dela nasce quando se abre a folha de ingredientes ou a de passos — eles
     * precisam de um pai onde se agarrar — e quem recua sem escrever nada deixa-a na base.
     * O que este teste guarda é que ela **entra na lista**: escondê-la fazia dela uma coisa
     * que ninguém consegue apagar, porque apagar passa por abri-la.
     */
    @Test
    fun `uma receita sem nome continua na lista`() {
        val lista = juntarRefeicoes(emptyList(), listOf(receita("")))

        assertEquals(1, lista.size)
        assertEquals("", lista.single().nome)
    }

    /** E fica no princípio, que é onde se dá por ela. */
    @Test
    fun `a receita sem nome fica no principio`() {
        val lista = juntarRefeicoes(listOf(modelo("Almoço")), listOf(receita("")))

        assertEquals("", lista.first().nome)
    }
}

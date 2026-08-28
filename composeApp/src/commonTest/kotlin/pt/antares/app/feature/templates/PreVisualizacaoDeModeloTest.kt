package pt.antares.app.feature.templates

import pt.antares.app.core.database.entities.MealTemplateEntity
import pt.antares.app.core.database.entities.MealTemplateItemEntity
import pt.antares.app.core.model.MealSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A escala da pré-visualização.
 *
 * **Era um campo de texto e passou a quatro chips**, que é o que o esboço da área 05
 * desenha. A troca não é de aspecto: o campo tinha estados intermédios que o partiam — o
 * vazio de quem apagou «1» para escrever «0,5», o zero que escrevia registos de zero
 * calorias, as letras que um teclado de hardware escreve num campo de números. Com quatro
 * valores fechados, **nenhum desses estados existe**, e é isso que estes testes guardam.
 */
class PreVisualizacaoDeModeloTest {

    private fun item(nome: String, gramas: Double, kcal: Int) = MealTemplateItemEntity(
        id = nome,
        templateId = "t",
        foodId = null,
        nameSnapshot = nome,
        quantityGrams = gramas,
        kcalSnapshot = kcal,
        proteinSnapshot = 10.0,
        carbsSnapshot = 20.0,
        fatSnapshot = 5.0,
        microsPer100Json = null,
        updatedAt = 0,
    )

    private fun pre() = PreVisualizacaoDeModelo(
        modelo = MealTemplateEntity(id = "t", name = "Almoço", slot = MealSlot.LUNCH, updatedAt = 0),
        itens = listOf(item("Arroz", 150.0, 200), item("Frango", 200.0, 330)),
    )

    @Test
    fun `por omissao vale uma vez`() {
        assertEquals(1.0, pre().multiplicador)
        assertEquals(530, pre().kcal)
    }

    @Test
    fun `meia refeicao vale metade`() {
        val p = pre().comEscala(0.5)
        assertEquals(0.5, p.multiplicador)
        assertEquals(265, p.kcal)
        assertEquals(75.0, p.gramasDe(p.itens.first()))
        assertEquals(100, p.kcalDe(p.itens.first()))
    }

    @Test
    fun `o dobro vale o dobro`() {
        val p = pre().comEscala(2.0)
        assertEquals(1060, p.kcal)
        assertEquals(300.0, p.gramasDe(p.itens.first()))
    }

    /**
     * O que fecha a porta que o campo de texto deixava aberta.
     *
     * Zero escrevia registos de zero calorias — ficavam no diário a somar nada e a ocupar a
     * lista. Negativo escrevia calorias negativas. Nenhum dos dois tem chip; e mesmo que
     * alguém chame o método com eles, o estado não muda.
     */
    @Test
    fun `uma escala de fora da lista nao entra`() {
        assertEquals(1.0, pre().comEscala(0.0).multiplicador)
        assertEquals(1.0, pre().comEscala(-2.0).multiplicador)
        assertEquals(1.0, pre().comEscala(1000.0).multiplicador)
        assertEquals(1.0, pre().comEscala(0.75).multiplicador)
    }

    /** Uma escala escolhida por engano desfaz-se escolhendo outra, sem passar pelo vazio. */
    @Test
    fun `trocar de escala substitui a anterior`() {
        assertEquals(1.5, pre().comEscala(0.5).comEscala(1.5).multiplicador)
    }

    @Test
    fun `as escalas sao as quatro do esboco`() {
        assertEquals(listOf(0.5, 1.0, 1.5, 2.0), PreVisualizacaoDeModelo.ESCALAS)
        assertTrue(PreVisualizacaoDeModelo.ESCALAS.all { it > 0 })
    }

    /** `×1,0` num chip lê-se como uma precisão que não existe. */
    @Test
    fun `o rotulo do chip perde a casa decimal dos inteiros`() {
        assertEquals("×1", rotuloDaEscala(1.0, virgula = true))
        assertEquals("×2", rotuloDaEscala(2.0, virgula = true))
        assertEquals("×0,5", rotuloDaEscala(0.5, virgula = true))
        assertEquals("×1,5", rotuloDaEscala(1.5, virgula = true))
    }

    @Test
    fun `o separador decimal segue o idioma`() {
        assertEquals("×0.5", rotuloDaEscala(0.5, virgula = false))
        assertEquals("×1", rotuloDaEscala(1.0, virgula = false))
    }

    @Test
    fun `uma refeicao vazia soma zero`() {
        assertEquals(0, pre().copy(itens = emptyList()).kcal)
    }
}

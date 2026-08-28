package pt.antares.app.feature.templates

import pt.antares.app.core.database.entities.MealTemplateEntity
import pt.antares.app.core.database.entities.MealTemplateItemEntity
import pt.antares.app.core.model.MealSlot
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * O multiplicador da pré-visualização.
 *
 * É um campo de texto que vale um número, e são os estados intermédios que o partem: o
 * campo vazio de quem apagou «1» para escrever «0,5», e as letras que um teclado de
 * hardware escreve num campo que só mostra números. Foi assim, na 2.17.0, que o campo de
 * gramas da folha da AI ficou a dizer «Ovos e» — e este nasce com a lição.
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

    private fun pre(texto: String = "1") = PreVisualizacaoDeModelo(
        modelo = MealTemplateEntity(id = "t", name = "Almoço", slot = MealSlot.LUNCH, updatedAt = 0),
        itens = listOf(item("Arroz", 150.0, 200), item("Frango", 200.0, 330)),
        multiplicadorTexto = texto,
    )

    @Test
    fun `por omissao vale uma vez`() {
        assertEquals(1.0, pre().multiplicador)
        assertEquals(530, pre().kcal)
    }

    @Test
    fun `meia refeicao vale metade`() {
        val p = pre("0,5")
        assertEquals(0.5, p.multiplicador)
        assertEquals(265, p.kcal)
        assertEquals(75.0, p.gramasDe(p.itens.first()))
        assertEquals(100, p.kcalDe(p.itens.first()))
    }

    /**
     * Um campo vazio vale um, e não zero.
     *
     * É por onde passa quem apaga para escrever outro número. Valer zero fazia a folha
     * mostrar «0 kcal» a meio de uma edição, e o botão prometer que ia escrever nada.
     */
    @Test
    fun `o campo vazio vale uma vez`() {
        assertEquals(1.0, pre("").multiplicador)
        assertEquals(530, pre("").kcal)
    }

    /**
     * Zero e negativo também valem um.
     *
     * Aplicar zero vezes escrevia registos de zero calorias — ficavam no diário a somar nada
     * e a ocupar a lista, que é pior do que não escrever nada.
     */
    @Test
    fun `zero e negativo valem uma vez`() {
        assertEquals(1.0, pre("0").multiplicador)
        assertEquals(1.0, pre("-2").multiplicador)
    }

    @Test
    fun `letras nao entram no campo`() {
        assertEquals("", pre().comTexto("duas").multiplicadorTexto)
        assertEquals("2", pre().comTexto("2x").multiplicadorTexto)
    }

    @Test
    fun `so ha um separador decimal`() {
        assertEquals("1,52", pre().comTexto("1,5,2").multiplicadorTexto)
    }

    /** Uma refeição vezes mil não é uma refeição; é um dedo preso no teclado. */
    @Test
    fun `o campo tem tecto`() {
        assertEquals(4, pre().comTexto("123456789").multiplicadorTexto.length)
    }

    @Test
    fun `uma refeicao vazia soma zero`() {
        assertEquals(0, pre().copy(itens = emptyList()).kcal)
    }
}

package pt.antares.app.feature.recipe

import pt.antares.app.core.calc.RecipeNutrition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Uma receita dá doses, e é por dose que se come. A app tinha o peso final — `yieldGrams` —
 * e mais nada: registar uma lasanha obrigava a saber quantos gramas dela se tinha comido, e
 * o ecrã abria com o peso da lasanha **inteira**, propondo comê-la toda.
 *
 * O que se guarda no diário continua a ser gramas. As doses são a forma de lá chegar.
 */
class RecipeDosesTest {

    private fun estado(
        doses: Int?,
        pesoTotal: Double,
        quantidade: String,
    ): RecipePortionState {
        val porDose = gramasPorDose(doses, pesoTotal)
        return RecipePortionState(
            loading = false,
            nutrition = RecipeNutrition(0, 0.0, 0.0, 0.0, pesoTotal),
            quantityText = quantidade,
            gramsPerServing = porDose,
            byServings = porDose != null,
        )
    }

    @Test
    fun `uma dose e o peso total a dividir pelas doses`() {
        assertEquals(300.0, gramasPorDose(doses = 4, basisGrams = 1200.0))
    }

    @Test
    fun `sem doses declaradas nao ha peso por dose`() {
        assertNull(gramasPorDose(doses = null, basisGrams = 1200.0))
    }

    @Test
    fun `zero doses nao divide por zero`() {
        // Não chega aqui pela interface, que filtra; chega de uma receita gravada por uma
        // versão anterior, ou de um valor escrito à mão na base.
        assertNull(gramasPorDose(doses = 0, basisGrams = 1200.0))
        assertNull(gramasPorDose(doses = 4, basisGrams = 0.0))
    }

    @Test
    fun `duas doses valem as gramas de duas doses`() {
        val s = estado(doses = 4, pesoTotal = 1200.0, quantidade = "2")

        assertTrue(s.byServings)
        assertEquals(600.0, s.quantityGrams, "é isto que vai para o diário, e não «2»")
    }

    @Test
    fun `sem doses o campo continua a contar gramas`() {
        val s = estado(doses = null, pesoTotal = 1200.0, quantidade = "250")

        assertTrue(!s.byServings)
        assertEquals(250.0, s.quantityGrams)
    }

    @Test
    fun `trocar para gramas leva a quantidade consigo`() {
        val emDoses = estado(doses = 4, pesoTotal = 1200.0, quantidade = "2")
        val emGramas = emDoses.trocarUnidade()

        assertTrue(!emGramas.byServings)
        assertEquals("600", emGramas.quantityText)
        assertEquals(600.0, emGramas.quantityGrams)

        val deVolta = emGramas.trocarUnidade()
        assertTrue(deVolta.byServings)
        assertEquals("2", deVolta.quantityText, "a volta perdeu a quantidade pelo caminho")
    }

    @Test
    fun `meia dose conta, um quarto nao`() {
        // 150 g numa receita de doses de 300 g é meia dose. 100 g são um terço, e arredonda
        // para meia — a alternativa era um campo a mostrar «0,333».
        val meia = estado(doses = 4, pesoTotal = 1200.0, quantidade = "150").copy(byServings = false)
        assertEquals("0.5", meia.trocarUnidade().quantityText)

        val terco = estado(doses = 4, pesoTotal = 1200.0, quantidade = "100").copy(byServings = false)
        assertEquals("0.5", terco.trocarUnidade().quantityText)
    }

    @Test
    fun `sem doses a troca nao faz nada`() {
        val s = estado(doses = null, pesoTotal = 1200.0, quantidade = "250")

        assertEquals(s, s.trocarUnidade(), "trocou para uma unidade que a receita não tem")
    }

    @Test
    fun `uma quantidade absurda continua a ser recusada`() {
        // Cinquenta doses de 300 g são quinze quilos. O teto vale em doses como valia em
        // gramas: é o mesmo número que o botão de guardar lê.
        val s = estado(doses = 4, pesoTotal = 1200.0, quantidade = "50")

        assertNull(s.quantityGrams)
    }
}

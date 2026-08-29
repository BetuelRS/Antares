package pt.antares.app.core.nutrition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * De onde veio **este** nutriente, e não o alimento inteiro.
 *
 * O esboço 22 pede a origem por nutriente, e a razão é a fusão por prioridade: um alimento do
 * INSA pode levar o iodo da CIQUAL, e um da CIQUAL pode levar metade dos micros do USDA. Até
 * à v36 o ecrã dizia uma origem só — a de quem deu o nome e as calorias — e calava a dos
 * outros números.
 *
 * O que estes testes guardam é o **formato compacto**: o oleoduto escreve só a excepção, e a
 * ausência quer dizer «veio de onde veio o alimento». Se alguém trocar isso por um mapa
 * completo, a coluna cresce oito mil vezes e a ausência passa a querer dizer o contrário.
 */
class OrigemPorNutrienteTest {

    @Test
    fun `sem coluna nenhuma, a origem e a do alimento`() {
        val origens = origensDeJson(null)

        assertTrue(origens.isEmpty())
        assertEquals(
            FoodProvenance.TCA,
            origemDoNutriente("iodine_ug", origens, FoodProvenance.TCA),
        )
    }

    @Test
    fun `a excepcao ganha a origem do alimento`() {
        val origens = origensDeJson("""{"iodine_ug":"CIQUAL"}""")

        assertEquals(
            FoodProvenance.CIQUAL,
            origemDoNutriente("iodine_ug", origens, FoodProvenance.TCA),
            "o iodo veio da CIQUAL mesmo num alimento do INSA — e um ecrã que diga «INSA» " +
                "nesta linha está a dizer de onde veio o nome, não de onde veio o número",
        )
        assertEquals(
            FoodProvenance.TCA,
            origemDoNutriente("calcium_mg", origens, FoodProvenance.TCA),
            "o que não está marcado veio de onde veio o alimento",
        )
    }

    /**
     * O catálogo actualiza-se sozinho e pode chegar de uma versão mais recente do que a app.
     * Uma origem que ela não conheça é uma linha sem marca — nunca um alimento sem
     * micronutrientes, que é como isto falharia se a leitura rebentasse.
     */
    @Test
    fun `uma origem desconhecida e deitada fora, e o resto sobrevive`() {
        val origens = origensDeJson("""{"iodine_ug":"CIQUAL","zinc_mg":"FONTE_DO_FUTURO"}""")

        assertEquals(mapOf("iodine_ug" to FoodProvenance.CIQUAL), origens)
    }

    @Test
    fun `texto que nao abre nao leva os micronutrientes atras`() {
        assertTrue(origensDeJson("{isto não é json").isEmpty())
        assertTrue(origensDeJson("").isEmpty())
    }

    /**
     * As duas colunas leem-se do mesmo objecto noutros sítios da app, e a diferença é o tipo
     * do valor. Aqui os valores são todos texto — como nos estados —, e por isso vale a pena
     * provar que uma coluna de origens não é lida como se fosse de números.
     */
    @Test
    fun `numeros nao passam por origens`() {
        assertTrue(origensDeJson("""{"iron_mg":2.3}""").isEmpty())
    }
}

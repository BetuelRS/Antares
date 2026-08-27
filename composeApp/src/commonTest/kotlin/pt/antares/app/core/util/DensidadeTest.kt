package pt.antares.app.core.util

import pt.antares.app.core.model.UnitSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Um mililitro não é uma grama.
 *
 * A app guarda gramas — é em gramas que a nutrição está medida, e a conta do dia é
 * `gramas ÷ 100`. Num líquido mostra mililitros, e até aqui os dois números eram o mesmo.
 * Para a água está certo por definição; para o azeite não: 200 ml pesam 182 g, e a app
 * contava-lhes 200. São 9 % de calorias a mais em cada colher.
 *
 * O que estes testes protegem é a ida e a volta: o que se escreve tem de voltar igual, ou a
 * quantidade muda sozinha de cada vez que se abre o registo.
 */
class DensidadeTest {

    private val azeite = 0.918
    private val mel = 1.42

    @Test
    fun `sem densidade um mililitro vale uma grama`() {
        val guardado = UnitConversions.portionToStored(200.0, UnitSystem.METRIC, liquid = true)
        assertEquals(200.0, guardado, 1e-9)
    }

    @Test
    fun `duzentos mililitros de azeite pesam cento e oitenta e dois gramas`() {
        val guardado = UnitConversions.portionToStored(200.0, UnitSystem.METRIC, true, azeite)
        assertEquals(183.6, guardado, 1e-9)

        assertTrue(guardado < 200.0, "o azeite é menos denso do que a água")
    }

    /** O mel é mais denso do que a água: uma colher pesa mais do que o volume dela. */
    @Test
    fun `cem mililitros de mel pesam cento e quarenta e dois gramas`() {
        val guardado = UnitConversions.portionToStored(100.0, UnitSystem.METRIC, true, mel)
        assertEquals(142.0, guardado, 1e-9)
    }

    @Test
    fun `o que se guarda volta a ler-se igual`() {
        val escrito = 250.0
        val guardado = UnitConversions.portionToStored(escrito, UnitSystem.METRIC, true, azeite)
        val lido = UnitConversions.portionToDisplay(guardado, UnitSystem.METRIC, true, azeite)

        assertEquals(escrito, lido, 1e-9)
    }

    /** E o mesmo em imperial, onde há duas conversões encadeadas. */
    @Test
    fun `a ida e a volta tambem fecham em imperial`() {
        val escrito = 8.0
        val guardado = UnitConversions.portionToStored(escrito, UnitSystem.IMPERIAL, true, azeite)
        val lido = UnitConversions.portionToDisplay(guardado, UnitSystem.IMPERIAL, true, azeite)

        assertEquals(escrito, lido, 1e-9)
    }

    /**
     * Um sólido não tem densidade a aplicar, mesmo que alguém lhe escreva uma.
     *
     * O que se escreve num sólido já são gramas: convertê-las era dividir a comida por um
     * número sem razão nenhuma.
     */
    @Test
    fun `um solido ignora a densidade`() {
        val guardado = UnitConversions.portionToStored(200.0, UnitSystem.METRIC, false, azeite)
        assertEquals(200.0, guardado, 1e-9)
    }

    /**
     * Uma densidade zero ou negativa não é uma medição — é um erro que dividia por zero.
     *
     * Cai no valor da água, que é o que a app assumia antes de a coluna existir.
     */
    @Test
    fun `uma densidade impossivel vale como agua`() {
        assertEquals(
            200.0,
            UnitConversions.portionToStored(200.0, UnitSystem.METRIC, true, 0.0),
            1e-9,
        )
        assertEquals(
            200.0,
            UnitConversions.portionToStored(200.0, UnitSystem.METRIC, true, -1.0),
            1e-9,
        )
    }

    @Test
    fun `a agua vale exactamente um`() {
        assertEquals(1.0, UnitConversions.DENSIDADE_DA_AGUA, 1e-12)

        val guardado = UnitConversions.portionToStored(500.0, UnitSystem.METRIC, true, 1.0)
        assertEquals(500.0, guardado, 1e-9)
    }
}

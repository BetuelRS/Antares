package pt.antares.app.feature.running

import pt.antares.app.core.model.UnitSystem
import pt.antares.app.feature.running.ui.RunFormat
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A distância tinha a vírgula escrita à mão, e o resto da app escolhe o separador pelo
 * idioma. Em inglês lia-se «6,87 mi» ao lado de «153.9 lb» no mesmo cartão do Hoje — visto no
 * emulador, não nos testes.
 *
 * O separador é o argumento, e é por isso que este teste o passa nos dois estados: um valor
 * por omissão traria de volta exatamente o defeito que isto guarda.
 */
class RunFormatTest {

    @Test
    fun `o separador decimal e o que lhe derem`() {
        val dezMilMetros = 10_000.0
        assertEquals("10,00", RunFormat.distance(dezMilMetros, UnitSystem.METRIC, comma = true))
        assertEquals("10.00", RunFormat.distance(dezMilMetros, UnitSystem.METRIC, comma = false))
    }

    @Test
    fun `em imperial a distancia vem em milhas`() {
        // 10 km são 6,21 milhas: o número muda, e não só o rótulo ao lado dele.
        assertEquals("6.21", RunFormat.distance(10_000.0, UnitSystem.IMPERIAL, comma = false))
    }

    @Test
    fun `duas casas sempre, mesmo quando a segunda e zero`() {
        // «5,1 km» e «5,10 km» numa lista de corridas fazem as colunas dançar.
        assertEquals("5.10", RunFormat.distance(5_100.0, UnitSystem.METRIC, comma = false))
        assertEquals("0.00", RunFormat.distance(0.0, UnitSystem.METRIC, comma = false))
    }
}

package pt.antares.app.core

import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.UnitConversions
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeightDisplayTest {

    private fun assertPerto(esperado: Double, obtido: Double, porque: String) {
        assertTrue(
            abs(esperado - obtido) < 0.01,
            "$porque — esperava ~$esperado, veio $obtido",
        )
    }

    @Test
    fun `em metrico o valor guardado passa intacto`() {

        assertEquals(70.0, UnitConversions.weightToDisplay(70.0, UnitSystem.METRIC))
        assertEquals(110.4, UnitConversions.weightToDisplay(110.4, UnitSystem.METRIC))
    }

    @Test
    fun `em imperial o valor converte-se para libras`() {
        assertPerto(154.32, UnitConversions.weightToDisplay(70.0, UnitSystem.IMPERIAL), "70 kg são ~154,3 lb")
        assertPerto(242.51, UnitConversions.weightToDisplay(110.0, UnitSystem.IMPERIAL), "110 kg são ~242,5 lb")
    }

    @Test
    fun `converter e voltar da o mesmo peso`() {

        val kg = 82.7
        val lb = UnitConversions.weightToDisplay(kg, UnitSystem.IMPERIAL)
        assertPerto(kg, UnitConversions.lbToKg(lb), "ida e volta tem de fechar")
    }

    @Test
    fun `o cartao do peso no Hoje troca rotulo e valor`() {
        // Os fins de linha normalizados antes de procurar o fim da função: com CRLF, o `"\n}\n"`
        // não aparecia, a janela passava a ser o resto do ficheiro, e o teste passava sempre.
        val hoje = File("src/commonMain/kotlin/pt/antares/app/feature/today/TodayScreen.kt")
            .readText().replace("\r\n", "\n")
        // O corpo da função do cartão, e não o texto entre o título e o convite de registo.
        // Olhava para essa janela até à 2.32.1, que passou o peso a cartão pequeno e calcula a
        // unidade antes de escrever o título: a decisão continuava cumprida, e o teste deixou de
        // a ver. Uma âncora que depende da ordem das linhas guarda a ordem, não a decisão.
        val cartao = hoje.substringAfter("private fun PequenoDoPeso").substringBefore("\n}\n")
        assertTrue(cartao.isNotEmpty(), "não encontrei o cartão do peso — o teste deixou de olhar para o sítio certo")

        assertTrue(
            cartao.contains("Res.string.common_lb") && cartao.contains("Res.string.common_kg"),
            "o cartão só conhece uma unidade; tem de escolher entre `kg` e `lb`",
        )
        assertTrue(
            cartao.contains("weightToDisplay"),
            "o cartão troca o rótulo sem converter o número — mostraria `70,0 lb` a quem pesa 70 kg",
        )
    }
}

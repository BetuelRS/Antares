package pt.antares.app.core.nutrition

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * O «mais ou menos» de um dia de comida.
 *
 * O que estes testes guardam não é a aritmética — é a **honestidade da aritmética**. Duas
 * doses do mesmo arroz partilham o desvio da mesma tabela, e tratá-las como medições
 * independentes encolhe o intervalo por um factor de raiz de dois sem razão nenhuma. Um
 * intervalo demasiado apertado é pior do que não ter intervalo: dá confiança em vez de a
 * calibrar.
 */
class IncertezaDaComidaTest {

    private fun parcela(id: String, origem: FoodProvenance, kcal: Double) =
        IncertezaDaComida.Parcela(id, origem, kcal)

    @Test
    fun `uma tabela nacional erra menos do que um palpite`() {
        assertTrue(IncertezaDaComida.de(FoodProvenance.CIQUAL) < IncertezaDaComida.de(FoodProvenance.OFF))
        assertTrue(IncertezaDaComida.de(FoodProvenance.OFF) < IncertezaDaComida.de(FoodProvenance.AI))
    }

    @Test
    fun `duas doses do mesmo alimento nao valem duas medicoes`() {

        // Se a tabela está 10 % alta no arroz, está 10 % alta nas duas doses. Somam-se, e o
        // intervalo de 400 kcal de arroz é 40 — não 28, que é o que a quadratura daria.
        val duasDoses = IncertezaDaComida.doDia(
            listOf(parcela("arroz", FoodProvenance.CIQUAL, 200.0), parcela("arroz", FoodProvenance.CIQUAL, 200.0)),
        )

        assertPerto(40.0, duasDoses.maisOuMenos)
    }

    @Test
    fun `alimentos diferentes somam-se em quadratura`() {

        // Duas análises diferentes erram em direções independentes: 20² + 20² = 800, e a
        // raiz disso são 28,3 — menos do que os 40 de somar às cegas.
        val doisAlimentos = IncertezaDaComida.doDia(
            listOf(parcela("arroz", FoodProvenance.CIQUAL, 200.0), parcela("feijao", FoodProvenance.CIQUAL, 200.0)),
        )

        assertPerto(28.284, doisAlimentos.maisOuMenos)
    }

    @Test
    fun `a percentagem do dia sai das parcelas e nao de uma media`() {
        val dia = IncertezaDaComida.doDia(
            listOf(
                parcela("arroz", FoodProvenance.CIQUAL, 800.0),
                parcela("foto", FoodProvenance.AI, 200.0),
            ),
        )

        assertPerto(1000.0, dia.kcal)

        // 80 e 60 em quadratura são 100: dez por cento do dia. As duzentas kcal adivinhadas
        // pesam no intervalo muito mais do que pesam no total.
        assertPerto(100.0, dia.maisOuMenos)
        assertPerto(0.10, dia.percentagem)
    }

    @Test
    fun `o dia diz quanto dele foi adivinhado`() {

        // Um intervalo sozinho não distingue um dia pesado de um dia adivinhado. Metade das
        // calorias vinda de uma fotografia é uma coisa que se tem de poder dizer.
        val metade = IncertezaDaComida.doDia(
            listOf(
                parcela("arroz", FoodProvenance.CIQUAL, 500.0),
                parcela("foto", FoodProvenance.AI, 500.0),
            ),
        )

        assertPerto(0.5, metade.fraccaoAdivinhada)
    }

    @Test
    fun `um alimento escrito a mao conta como estimativa`() {

        // Foi estimado a partir de uma receita, não medido. Os ecrãs já o dizem com o
        // `verified = false`; aqui diz-se em número.
        val curado = IncertezaDaComida.doDia(listOf(parcela("ptx_feijoada", FoodProvenance.CURATED, 600.0)))

        assertPerto(1.0, curado.fraccaoAdivinhada)
    }

    @Test
    fun `um defice mais pequeno do que o erro nao e um defice observado`() {
        val dia = IncertezaDaComida.doDia(listOf(parcela("arroz", FoodProvenance.CIQUAL, 2000.0)))

        // ±200 kcal. Um défice de 150 desaparece dentro deles; um de 400 não.
        assertTrue(dia.menorDoQueOErro(150.0), "um dia com 200 de incerteza não observa 150")
        assertFalse(dia.menorDoQueOErro(400.0))
    }

    @Test
    fun `um dia vazio nao tem intervalo nenhum`() {
        val vazio = IncertezaDaComida.doDia(emptyList())

        assertEquals(0.0, vazio.kcal)
        assertEquals(0.0, vazio.maisOuMenos)
        assertEquals(0.0, vazio.percentagem)
        assertFalse(vazio.menorDoQueOErro(100.0), "sem comida não há nada que o erro esconda")
    }

    private fun assertPerto(esperado: Double, obtido: Double) {
        assertTrue(abs(esperado - obtido) < TOLERANCIA, "esperava $esperado, veio $obtido")
    }

    private companion object {
        const val TOLERANCIA = 0.01
    }
}

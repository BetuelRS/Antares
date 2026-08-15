package pt.antares.app.core.calc

import pt.antares.app.core.util.MINUTES_PER_HOUR
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Os padrões que só a hora revela, e sobretudo **quando é que a app se cala sobre eles**.
 *
 * Todo o histórico anterior à coluna da hora não tem horas nenhumas. Se estes padrões
 * aparecessem na mesma, a app estaria a descrever hábitos que nunca mediu.
 */
class PadroesPorHoraTest {

    private val segunda = 20_003L

    private fun h(hora: Int, minuto: Int = 0) = hora * MINUTES_PER_HOUR + minuto

    private fun dias(quantos: Int, horas: List<Int?>) = (0 until quantos).map {
        EatingPatterns.Day(
            epochDay = segunda + it,
            kcal = 2000.0,
            proteinG = 100.0,
            horas = horas,
        )
    }

    private fun tipos(padroes: List<EatingPatterns.Pattern>) = padroes.map { it.kind }.toSet()

    @Test
    fun `uma janela larga e descrita, com a duracao em minutos`() {
        val padroes = EatingPatterns.detect(dias(20, listOf(h(7), h(23))))

        val janela = padroes.single { it.kind == EatingPatterns.Kind.LONG_EATING_WINDOW }
        assertEquals(16 * MINUTES_PER_HOUR, janela.value, "o valor é a duração, em minutos")
    }

    @Test
    fun `uma janela curta nao da padrao nenhum`() {
        val padroes = EatingPatterns.detect(dias(20, listOf(h(12), h(20))))

        assertTrue(
            EatingPatterns.Kind.LONG_EATING_WINDOW !in tipos(padroes),
            "oito horas de janela não é coisa que valha a pena nomear",
        )
    }

    @Test
    fun `a ultima refeicao tardia traz a hora, e nao a duracao`() {
        val padroes = EatingPatterns.detect(dias(20, listOf(h(12), h(22, 40))))

        val tardia = padroes.single { it.kind == EatingPatterns.Kind.LATE_LAST_MEAL }
        assertEquals(h(22, 40), tardia.value)
    }

    @Test
    fun `jantar as oito nao e jantar tardio`() {
        val padroes = EatingPatterns.detect(dias(20, listOf(h(12), h(20))))
        assertTrue(EatingPatterns.Kind.LATE_LAST_MEAL !in tipos(padroes))
    }

    @Test
    fun `um historico sem horas nao inventa padroes por hora`() {
        val padroes = EatingPatterns.detect(dias(30, listOf(null, null, null)))

        assertTrue(
            EatingPatterns.Kind.LONG_EATING_WINDOW !in tipos(padroes) &&
                EatingPatterns.Kind.LATE_LAST_MEAL !in tipos(padroes),
            "descreveu hábitos de hora num histórico que não tem horas nenhumas",
        )
    }

    @Test
    fun `menos de uma semana com horas ainda nao e habito`() {
        // Dias suficientes para os padrões antigos, mas só seis com hora.
        val comHora = dias(6, listOf(h(7), h(23)))
        val semHora = (6 until 20).map {
            EatingPatterns.Day(segunda + it, kcal = 2000.0, proteinG = 100.0, horas = listOf(null))
        }

        val padroes = EatingPatterns.detect(comHora + semHora)
        assertTrue(EatingPatterns.Kind.LONG_EATING_WINDOW !in tipos(padroes))
    }

    @Test
    fun `os padroes por refeicao continuam a funcionar sem horas`() {
        // O mesmo cenário de concentração de sempre, sem uma única hora registada.
        val dias = (0 until 20).map {
            EatingPatterns.Day(
                epochDay = segunda + it,
                kcal = 2000.0,
                proteinG = 100.0,
                kcalBySlot = mapOf("DINNER" to 1600.0, "LUNCH" to 400.0),
            )
        }

        assertTrue(
            EatingPatterns.Kind.MEAL_CONCENTRATION in tipos(EatingPatterns.detect(dias)),
            "acrescentar padrões por hora não pode calar os que já existiam",
        )
    }
}

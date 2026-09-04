package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SeriesPorMusculoTest {

    @Test
    fun `uma serie conta inteira para cada musculo primario`() {
        val contas = SeriesPorMusculo.contar(
            listOf(
                listOf("chest", "triceps"),
                listOf("chest"),
            ),
        )
        assertEquals(2, contas["chest"])
        assertEquals(1, contas["triceps"])
    }

    /**
     * O total da coluna é maior do que o número de séries, e é esperado: repartir pelo número
     * de músculos subavaliava o trabalho de cada um nos compostos.
     */
    @Test
    fun `o total por musculo passa o numero de series nos compostos`() {
        val contas = SeriesPorMusculo.contar(listOf(listOf("chest", "triceps", "shoulders")))
        assertEquals(3, contas.values.sum())
    }

    @Test
    fun `uma serie sem musculo declarado nao desaparece`() {
        val contas = SeriesPorMusculo.contar(listOf(emptyList()))
        assertEquals(1, contas[MuscleVolume.OTHER])
    }

    @Test
    fun `a media semanal de um mes divide pelas semanas que ele tem`() {
        // 60 séries em 30 dias são 4,28 semanas: 14 por semana.
        assertEquals(14, SeriesPorMusculo.porSemana(total = 60, diasDoPeriodo = 30))
    }

    @Test
    fun `uma semana e ela propria`() {
        assertEquals(16, SeriesPorMusculo.porSemana(total = 16, diasDoPeriodo = 7))
    }

    /**
     * Um dia não vira uma semana. Multiplicar por sete inventava seis dias que não
     * aconteceram, e a faixa é semanal — sem semana não há com que comparar.
     */
    @Test
    fun `um periodo mais curto do que uma semana nao da media semanal`() {
        assertNull(SeriesPorMusculo.porSemana(total = 5, diasDoPeriodo = 1))
    }

    @Test
    fun `a faixa separa abaixo dentro e acima`() {
        assertEquals(SeriesPorMusculo.Faixa.ABAIXO, SeriesPorMusculo.faixaDe(9))
        assertEquals(SeriesPorMusculo.Faixa.DENTRO, SeriesPorMusculo.faixaDe(10))
        assertEquals(SeriesPorMusculo.Faixa.DENTRO, SeriesPorMusculo.faixaDe(20))
        assertEquals(SeriesPorMusculo.Faixa.ACIMA, SeriesPorMusculo.faixaDe(21))
    }
}

class FrequenciaDeTreinoTest {

    // 2026-09-03 é uma quinta-feira; a semana ISO começa a 2026-08-31 (segunda).
    private val quinta = 20699L
    private val segunda = 20696L

    @Test
    fun `os treinos caem na semana ISO a que pertencem`() {
        val serie = FrequenciaDeTreino.porSemana(
            iniciosDeTreino = listOf(segunda, quinta),
            hojeEpochDay = quinta,
            semanas = 1,
        )
        assertEquals(listOf(2), serie)
    }

    /**
     * O domingo anterior pertence à semana de trás, e não à de agora. É a diferença entre a
     * semana ISO e sete dias para trás — e era essa a incoerência deste ecrã.
     */
    @Test
    fun `o domingo antes de segunda e outra semana`() {
        val domingo = segunda - 1
        val serie = FrequenciaDeTreino.porSemana(
            iniciosDeTreino = listOf(domingo, quinta),
            hojeEpochDay = quinta,
            semanas = 2,
        )
        assertEquals(listOf(1, 1), serie)
    }

    /** As semanas sem treino entram com zero: uma paragem de um mês tem de se ver. */
    @Test
    fun `as semanas vazias entram com zero`() {
        val serie = FrequenciaDeTreino.porSemana(
            iniciosDeTreino = listOf(quinta),
            hojeEpochDay = quinta,
            semanas = 4,
        )
        assertEquals(listOf(0, 0, 0, 1), serie)
    }

    @Test
    fun `um treino fora da janela nao conta`() {
        val muitoAntes = quinta - 400
        val serie = FrequenciaDeTreino.porSemana(
            iniciosDeTreino = listOf(muitoAntes, quinta),
            hojeEpochDay = quinta,
            semanas = 4,
        )
        assertEquals(1, serie.sum())
    }

    @Test
    fun `a media tem uma casa decimal`() {
        assertEquals(3.4, FrequenciaDeTreino.media(listOf(3, 4, 3, 4, 3)))
    }

    @Test
    fun `sem semanas nao ha media`() {
        assertEquals(0.0, FrequenciaDeTreino.media(emptyList()))
    }
}

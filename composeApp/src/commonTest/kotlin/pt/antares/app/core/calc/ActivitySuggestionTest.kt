package pt.antares.app.core.calc

import pt.antares.app.core.model.ActivityLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActivitySuggestionTest {

    private fun dias(passos: Long, n: Int = 14) = List(n) { passos }

    @Test
    fun `abaixo de cinco mil e sedentario`() {
        assertEquals(ActivityLevel.SEDENTARY, ActivitySuggestion.levelForSteps(4_999))
        assertEquals(ActivityLevel.SEDENTARY, ActivitySuggestion.levelForSteps(0))
    }

    @Test
    fun `cinco mil ja e leve`() {

        assertEquals(ActivityLevel.LIGHT, ActivitySuggestion.levelForSteps(5_000))
        assertEquals(ActivityLevel.LIGHT, ActivitySuggestion.levelForSteps(7_499))
    }

    @Test
    fun `sete mil e quinhentos e moderado`() {
        assertEquals(ActivityLevel.MODERATE, ActivitySuggestion.levelForSteps(7_500))
        assertEquals(ActivityLevel.MODERATE, ActivitySuggestion.levelForSteps(9_999))
    }

    @Test
    fun `dez mil ou mais e elevado`() {
        assertEquals(ActivityLevel.HIGH, ActivitySuggestion.levelForSteps(10_000))
        assertEquals(ActivityLevel.HIGH, ActivitySuggestion.levelForSteps(12_500))
    }

    @Test
    fun `os passos nunca propoem atleta`() {

        assertEquals(ActivityLevel.HIGH, ActivitySuggestion.levelForSteps(30_000))
        assertEquals(ActivityLevel.HIGH, ActivitySuggestion.levelForSteps(Long.MAX_VALUE / 2))
    }

    @Test
    fun `menos de uma semana de dados nao diz nada`() {
        assertNull(ActivitySuggestion.averageDailySteps(dias(12_000, n = 6)))
        assertNull(ActivitySuggestion.suggest(dias(12_000, n = 6), ActivityLevel.SEDENTARY))
    }

    @Test
    fun `uma semana certa ja chega`() {
        assertEquals(12_000L, ActivitySuggestion.averageDailySteps(dias(12_000, n = 7)))
    }

    @Test
    fun `dias sem passos nao contam para a media`() {

        val comBuracos = List(7) { 10_000L } + List(7) { 0L }
        assertEquals(10_000L, ActivitySuggestion.averageDailySteps(comBuracos))
    }

    @Test
    fun `dias a zero tambem nao contam para o minimo`() {

        val poucos = List(6) { 10_000L } + List(8) { 0L }
        assertNull(ActivitySuggestion.averageDailySteps(poucos))
    }

    @Test
    fun `sem passos nenhuns nao ha sugestao`() {
        assertNull(ActivitySuggestion.suggest(emptyList(), ActivityLevel.MODERATE))
        assertNull(ActivitySuggestion.suggest(dias(0), ActivityLevel.MODERATE))
    }

    @Test
    fun `nao propoe o nivel que a pessoa ja tem`() {
        assertNull(ActivitySuggestion.suggest(dias(8_000), ActivityLevel.MODERATE))
    }

    @Test
    fun `propoe descer a quem escolheu de mais`() {

        val s = ActivitySuggestion.suggest(dias(4_000), ActivityLevel.HIGH)
        assertEquals(ActivityLevel.SEDENTARY, s?.suggested)
        assertEquals(4_000L, s?.averageSteps)
        assertEquals(ActivityLevel.HIGH, s?.current)
    }

    @Test
    fun `propoe subir a quem anda mais do que julgava`() {
        assertEquals(
            ActivityLevel.HIGH,
            ActivitySuggestion.suggest(dias(11_000), ActivityLevel.SEDENTARY)?.suggested,
        )
    }

    @Test
    fun `quem escolheu atleta nao e incomodado`() {

        assertNull(ActivitySuggestion.suggest(dias(3_000), ActivityLevel.ATHLETE))
        assertNull(ActivitySuggestion.suggest(dias(20_000), ActivityLevel.ATHLETE))
    }

    @Test
    fun `sem nivel escolhido ainda ha sugestao`() {
        assertEquals(
            ActivityLevel.MODERATE,
            ActivitySuggestion.suggest(dias(8_000), current = null)?.suggested,
        )
    }
}

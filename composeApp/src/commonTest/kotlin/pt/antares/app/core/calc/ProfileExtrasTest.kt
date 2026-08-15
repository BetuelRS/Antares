package pt.antares.app.core.calc

import pt.antares.app.core.database.entities.BodyMeasurementEntity
import pt.antares.app.core.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileExtrasTest {

    @Test
    fun `FFMI e massa magra a dividir pela altura ao quadrado`() {

        assertEquals(20.2, BodyComposition.ffmi(64.0, 178)!!, 0.05)
    }

    @Test
    fun `sem massa magra nao ha FFMI`() {
        assertNull(BodyComposition.ffmi(0.0, 178))
        assertNull(BodyComposition.ffmi(64.0, 0))
    }

    @Test
    fun `o FFMI aparece no stats quando ha gordura conhecida`() {
        val s = BodyComposition.stats(
            sex = pt.antares.app.core.model.Sex.MALE,
            weightKg = 80.0, heightCm = 178, ageYears = 30,
            bodyFatPct = 20.0,
            bodyFatSource = pt.antares.app.core.model.BodyFatSource.MEASURED,
        )
        assertEquals(BodyComposition.ffmi(64.0, 178)!!, s.ffmi!!, 0.001)
    }

    @Test
    fun `Cunningham e 500 mais 22 por kg de massa magra`() {

        assertEquals(1996.0, NutritionCalc.bmrCunningham(68.0), 0.01)
    }

    @Test
    fun `Cunningham da mais que Katch para a mesma massa magra`() {

        val lean = 68.0
        assertTrue(NutritionCalc.bmrCunningham(lean) > NutritionCalc.bmrKatchMcArdle(lean))
    }

    @Test
    fun `agua parte da EFSA e distingue homens de mulheres`() {
        // Mudança intencional: eram 35 ml por quilo, iguais para os dois sexos.
        assertTrue(
            DailyGoals.waterMl(Sex.MALE, 80.0) > DailyGoals.waterMl(Sex.FEMALE, 80.0),
            "a EFSA dá 2,5 L aos homens e 2,0 L às mulheres, e a app era cega a isso",
        )
    }

    @Test
    fun `sem peso valido a meta de agua e zero, nao um palpite`() {
        assertEquals(0, DailyGoals.waterMl(Sex.MALE, 0.0))
    }

    @Test
    fun `a fibra nao escala com o peso`() {

        assertEquals(25, DailyGoals.fibreG())
    }

    @Test
    fun `a menores pergunta-se de meio em meio ano`() {
        val hoje = 20_000L
        assertTrue(
            HeightCheck.isDue(16, confirmedEpochDay = hoje - 200, profileUpdatedEpochDay = hoje, todayEpochDay = hoje),
        )
        assertFalse(
            HeightCheck.isDue(16, confirmedEpochDay = hoje - 100, profileUpdatedEpochDay = hoje, todayEpochDay = hoje),
        )
    }

    @Test
    fun `a adultos pergunta-se muito de longe`() {
        val hoje = 20_000L

        assertFalse(
            HeightCheck.isDue(30, confirmedEpochDay = hoje - 200, profileUpdatedEpochDay = hoje, todayEpochDay = hoje),
        )
        assertTrue(
            HeightCheck.isDue(30, confirmedEpochDay = hoje - 800, profileUpdatedEpochDay = hoje, todayEpochDay = hoje),
        )
    }

    @Test
    fun `sem data de confirmacao usa a do perfil`() {

        val hoje = 20_000L
        assertTrue(
            HeightCheck.isDue(16, confirmedEpochDay = null, profileUpdatedEpochDay = hoje - 400, todayEpochDay = hoje),
        )
    }

    private fun m(day: Long, waist: Double? = null, fat: Double? = null) = BodyMeasurementEntity(
        id = "m$day", epochDay = day, waistCm = waist, bodyFatPct = fat, updatedAt = 0L,
    )

    @Test
    fun `com duas medicoes ha progresso`() {
        val p = MeasurementProgressCalc.compute(listOf(m(0, waist = 90.0), m(60, waist = 84.0)))!!
        assertEquals(-6.0, p.waistDelta!!, 0.001)
        assertTrue(p.isMeaningful)
    }

    @Test
    fun `uma medicao so nao da progresso nenhum`() {
        assertNull(MeasurementProgressCalc.compute(listOf(m(0, waist = 90.0))))
    }

    @Test
    fun `meio centimetro nao e progresso, e a fita metrica`() {
        val p = MeasurementProgressCalc.compute(listOf(m(0, waist = 90.0), m(30, waist = 89.6)))!!
        assertFalse(p.isMeaningful)
    }

    @Test
    fun `cada metrica compara os seus proprios extremos`() {

        val p = MeasurementProgressCalc.compute(
            listOf(m(0, fat = 24.0), m(30, fat = 22.0, waist = 92.0), m(60, fat = 20.0, waist = 88.0)),
        )!!
        assertEquals(-4.0, p.fatDelta!!, 0.001)
        assertEquals(-4.0, p.waistDelta!!, 0.001)
    }
}

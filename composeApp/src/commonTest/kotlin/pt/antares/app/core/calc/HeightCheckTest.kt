package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeightCheckTest {

    private val hoje = 20_639L

    @Test
    fun `um menor e reperguntado ao fim de meio ano`() {
        assertTrue(
            HeightCheck.isDue(
                ageYears = 16,
                confirmedEpochDay = hoje - HeightCheck.MINOR_INTERVAL_DAYS,
                profileUpdatedEpochDay = hoje - 1000,
                todayEpochDay = hoje,
            ),
        )
    }

    @Test
    fun `um menor confirmado ontem nao e incomodado`() {
        assertFalse(
            HeightCheck.isDue(
                ageYears = 16,
                confirmedEpochDay = hoje - 1,
                profileUpdatedEpochDay = hoje - 1000,
                todayEpochDay = hoje,
            ),
        )
    }

    @Test
    fun `um adulto so e reperguntado ao fim de dois anos`() {
        assertFalse(
            HeightCheck.isDue(
                ageYears = 30,
                confirmedEpochDay = hoje - HeightCheck.MINOR_INTERVAL_DAYS,
                profileUpdatedEpochDay = hoje - 1000,
                todayEpochDay = hoje,
            ),
        )
        assertTrue(
            HeightCheck.isDue(
                ageYears = 30,
                confirmedEpochDay = hoje - HeightCheck.ADULT_INTERVAL_DAYS,
                profileUpdatedEpochDay = hoje - 1000,
                todayEpochDay = hoje,
            ),
        )
    }

    @Test
    fun `sem confirmacao conta-se desde que o perfil foi escrito`() {
        assertFalse(
            HeightCheck.isDue(
                ageYears = 30,
                confirmedEpochDay = null,
                profileUpdatedEpochDay = hoje - 10,
                todayEpochDay = hoje,
            ),
        )
        assertTrue(
            HeightCheck.isDue(
                ageYears = 30,
                confirmedEpochDay = null,
                profileUpdatedEpochDay = hoje - HeightCheck.ADULT_INTERVAL_DAYS,
                todayEpochDay = hoje,
            ),
        )
    }

    @Test
    fun `o intervalo do adulto e maior do que o do menor`() {
        assertTrue(HeightCheck.ADULT_INTERVAL_DAYS > HeightCheck.MINOR_INTERVAL_DAYS)
    }
}

package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkoutCalcTest {

    @Test
    fun `epley calcula 1RM e respeita reps 1 a 12`() {

        assertEquals(100.0 * (1 + 1.0 / 30), OneRepMax.epley(100.0, 1)!!, 0.001)
        assertEquals(100.0 * (1 + 5.0 / 30), OneRepMax.epley(100.0, 5)!!, 0.001)

        assertNull(OneRepMax.epley(100.0, 13))

        assertNull(OneRepMax.epley(100.0, 0))
        assertNull(OneRepMax.epley(0.0, 5))
    }

    @Test
    fun `volume ignora warmups`() {
        val sets = listOf(
            SetEntry(60.0, 10, isWarmup = true),
            SetEntry(80.0, 8),
            SetEntry(80.0, 6),
        )
        assertEquals(80.0 * 8 + 80.0 * 6, VolumeCalc.volume(sets))
    }

    @Test
    fun `deteta PR de 1RM e de peso-reps em 3 sessoes`() {

        val s1 = listOf(SetEntry(80.0, 5))
        val pr1 = PrDetector.detect(previous = null, current = s1)
        assertTrue(pr1.newOneRm && pr1.newWeightReps)

        val best1 = PrDetector.best(s1)!!

        val s2 = listOf(SetEntry(85.0, 5))
        val pr2 = PrDetector.detect(previous = best1, current = s2)
        assertTrue(pr2.newOneRm)
        assertTrue(pr2.newWeightReps)

        val best2 = PrDetector.best(s2)!!

        val s3 = listOf(SetEntry(85.0, 3))
        val pr3 = PrDetector.detect(previous = best2, current = s3)
        assertFalse(pr3.any)
    }

    @Test
    fun `volume por musculo distribui a series primarias`() {
        val inputs = listOf(
            MuscleVolumeInput(80.0, 10, listOf("chest", "triceps")),
            MuscleVolumeInput(100.0, 5, listOf("quadriceps")),
            MuscleVolumeInput(50.0, 8, emptyList()),
        )
        val agg = MuscleVolume.aggregate(inputs)
        assertEquals(800.0, agg["chest"])
        assertEquals(800.0, agg["triceps"])
        assertEquals(500.0, agg["quadriceps"])
        assertEquals(400.0, agg[MuscleVolume.OTHER])
    }

    @Test
    fun `series longas nao produzem 1RM e isso e null, nao zero`() {
        val longas = listOf(SetEntry(weightKg = 40.0, reps = 20))
        val pr = PrDetector.best(longas)

        assertNotNull(pr, "há trabalho válido: peso x reps existe")
        assertNull(pr.bestOneRm, "20 reps não têm 1RM estimável — não pode ser 0.0")
        assertEquals(800.0, pr.bestWeightReps)
    }

    @Test
    fun `sem 1RM estimavel nao ha recorde de 1RM`() {
        val longas = listOf(SetEntry(weightKg = 40.0, reps = 20))
        assertFalse(PrDetector.detect(previous = null, current = longas).newOneRm)

        val anterior = PrDetector.best(listOf(SetEntry(weightKg = 100.0, reps = 5)))!!
        assertFalse(PrDetector.detect(previous = anterior, current = longas).newOneRm)
    }

    @Test
    fun `a primeira estimativa depois de series longas e recorde`() {
        val semEstimativa = PrDetector.best(listOf(SetEntry(weightKg = 40.0, reps = 20)))!!
        val comEstimativa = listOf(SetEntry(weightKg = 100.0, reps = 5))

        assertTrue(PrDetector.detect(previous = semEstimativa, current = comEstimativa).newOneRm)
    }
}

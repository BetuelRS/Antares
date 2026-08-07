package pt.antares.app.feature.achievements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AchievementCalcTest {

    @Test
    fun `desbloqueia patamares atingidos e mantem os futuros bloqueados`() {
        val stats = AchievementStats(workouts = 12, runKm = 0, fastsCompleted = 0, weighIns = 0)
        val workouts = AchievementCalc.build(stats).filter { it.category == AchievementCategory.WORKOUTS }

        assertTrue(workouts.first { it.target == 10 }.unlocked)
        assertFalse(workouts.first { it.target == 25 }.unlocked)
    }

    @Test
    fun `fracao satura em 1 e nunca passa`() {
        val a = Achievement(AchievementCategory.RUN_KM, target = 5, current = 20)
        assertEquals(1f, a.fraction)
        assertTrue(a.unlocked)
    }

    @Test
    fun `contagem de desbloqueadas e total coerentes`() {
        val none = AchievementStats()
        assertEquals(0, AchievementCalc.unlockedCount(none))

        assertEquals(22, AchievementCalc.total())

        val some = AchievementStats(
            workouts = 100, runKm = 250, fastsCompleted = 30, weighIns = 30,
            waistCmLost = 10, bodyFatPctLost = 5,
        )
        assertEquals(22, AchievementCalc.unlockedCount(some))
    }
}

class BodyAchievementsTest {

    @Test
    fun `sem medicoes que cheguem nao ha descida`() {
        assertEquals(0, AchievementCalc.bestDrop(emptyList()))
        assertEquals(0, AchievementCalc.bestDrop(listOf(92.0)))
    }

    @Test
    fun `mede do primeiro registo ate ao melhor`() {
        assertEquals(4, AchievementCalc.bestDrop(listOf(92.0, 90.5, 88.0)))
    }

    @Test
    fun `uma recaida nao apaga a medalha`() {

        assertEquals(5, AchievementCalc.bestDrop(listOf(92.0, 87.0, 90.0)))
    }

    @Test
    fun `subir nunca da conquista negativa`() {
        assertEquals(0, AchievementCalc.bestDrop(listOf(85.0, 88.0, 91.0)))
    }

    @Test
    fun `a cintura e a gordura entram no catalogo`() {
        val stats = AchievementStats(waistCmLost = 5, bodyFatPctLost = 3)
        val corpo = AchievementCalc.build(stats).filter {
            it.category == AchievementCategory.WAIST_CM ||
                it.category == AchievementCategory.BODY_FAT_PCT
        }
        assertTrue(corpo.isNotEmpty(), "as categorias do corpo têm de estar no catálogo")

        assertEquals(4, corpo.count { it.unlocked })
    }

    @Test
    fun `quem nunca mediu o corpo nao ve conquistas do corpo desbloqueadas`() {

        val zero = AchievementCalc.build(AchievementStats())
        assertEquals(
            0,
            zero.count {
                it.unlocked &&
                    (it.category == AchievementCategory.WAIST_CM ||
                        it.category == AchievementCategory.BODY_FAT_PCT)
            },
        )
    }
}

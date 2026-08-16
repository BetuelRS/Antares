package pt.antares.app.core.calc

import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeeklyAggregateTest {

    private val monday = 20_000L

    private fun food(day: Long, kcal: Int, protein: Double = 100.0) = FoodLogEntity(
        id = "f$day-$kcal",
        epochDay = day,
        mealSlot = MealSlot.LUNCH,
        foodId = null,
        nameSnapshot = "x",
        quantityGrams = 100.0,
        kcalSnapshot = kcal,
        proteinSnapshot = protein,
        carbsSnapshot = 50.0,
        fatSnapshot = 20.0,
        microsPer100Json = null,
        origin = LogOrigin.MANUAL,
        updatedAt = 0,
    )

    private fun weight(day: Long, kg: Double, deleted: Boolean = false) = WeightLogEntity(
        id = "w$day",
        epochDay = day,
        weightKg = kg,
        note = null,
        updatedAt = 0,
        deleted = deleted,
    )

    @Test
    fun `conta dias registados, media e aderencia`() {
        val logs = listOf(
            food(monday, 2000),
            food(monday + 1, 2100),
            food(monday + 2, 1000),
            food(monday + 3, 1500), food(monday + 3, 500),
        )
        val a = WeeklyAggregator.build(
            weekStartEpochDay = monday,
            foodLogs = logs,
            targetKcal = 2000,
            weights = emptyList(),
        )

        assertEquals(4, a.loggedDays)
        assertEquals(1775, a.avgKcal)
        assertEquals(3, a.daysOnTarget)
        assertEquals(monday + 6, a.weekEndEpochDay)
    }

    @Test
    fun `semana esparsa e sinalizada — o coach tem de o dizer`() {
        val a = WeeklyAggregator.build(
            weekStartEpochDay = monday,
            foodLogs = listOf(food(monday, 2000), food(monday + 1, 2000), food(monday + 2, 2000)),
            targetKcal = 2000,
            weights = emptyList(),
        )
        assertTrue(a.isSparse)
    }

    @Test
    fun `linhas apagadas nao contam para nada`() {
        val deleted = food(monday, 5000).copy(deleted = true)
        val a = WeeklyAggregator.build(
            weekStartEpochDay = monday,
            foodLogs = listOf(food(monday + 1, 2000), deleted),
            targetKcal = 2000,
            weights = listOf(weight(monday, 80.0, deleted = true), weight(monday + 1, 79.0)),
        )
        assertEquals(1, a.loggedDays)
        assertEquals(2000, a.avgKcal)
        assertEquals(1, a.weighIns)
    }

    @Test
    fun `tendencia do peso usa EMA, nao duas pesagens soltas`() {

        val a = WeeklyAggregator.build(
            weekStartEpochDay = monday,
            foodLogs = listOf(food(monday, 2000)),
            targetKcal = 2000,
            weights = listOf(
                weight(monday, 80.0),
                weight(monday + 2, 81.0),
                weight(monday + 4, 79.5),
                weight(monday + 6, 79.2),
            ),
        )
        assertEquals(4, a.weighIns)
        assertEquals(80.0, a.weightStartKg)
        assertEquals(79.2, a.weightEndKg)
        assertTrue(a.weightTrendDeltaKg!! < 0, "a tendência devia descer, veio ${a.weightTrendDeltaKg}")
    }

    @Test
    fun `com menos de duas pesagens nao ha tendencia nenhuma`() {
        val a = WeeklyAggregator.build(
            weekStartEpochDay = monday,
            foodLogs = listOf(food(monday, 2000)),
            targetKcal = 2000,
            weights = listOf(weight(monday, 80.0)),
        )
        assertNull(a.weightTrendDeltaKg)
    }

    @Test
    fun `semana vazia nao rebenta nem divide por zero`() {
        val a = WeeklyAggregator.build(
            weekStartEpochDay = monday,
            foodLogs = emptyList(),
            targetKcal = 2000,
            weights = emptyList(),
        )
        assertEquals(0, a.loggedDays)
        assertEquals(0, a.avgKcal)
        assertEquals(0, a.daysOnTarget)
        assertTrue(a.isSparse)
        assertEquals(emptyList(), a.diasComRegisto)
    }

    @Test
    fun `guarda quais os dias com registo, e nao so quantos`() {
        // Cinco dias seguidos e cinco dias alternados dão o mesmo `loggedDays`. O relatório
        // mostra a semana à vista, e sem saber quais eram desenhava a semana de outra pessoa.
        val a = WeeklyAggregator.build(
            weekStartEpochDay = monday,
            foodLogs = listOf(food(monday, 2000), food(monday + 2, 2000), food(monday + 5, 2000)),
            targetKcal = 2000,
            weights = emptyList(),
        )

        assertEquals(listOf(monday, monday + 2, monday + 5), a.diasComRegisto)
        assertEquals(a.loggedDays, a.diasComRegisto.size, "as duas contagens têm de bater")
    }
}

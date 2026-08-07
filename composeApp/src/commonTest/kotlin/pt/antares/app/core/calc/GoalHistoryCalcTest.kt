package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoalHistoryCalcTest {

    private val hoje = 20_650L

    @Test
    fun `atinge dentro da tolerancia`() {
        assertTrue(GoalHistoryCalc.reaches(targetKg = 78.0, weightKg = 78.0))
        assertTrue(GoalHistoryCalc.reaches(78.0, 78.2))
        assertTrue(GoalHistoryCalc.reaches(78.0, 77.8))
    }

    @Test
    fun `nao atinge fora da tolerancia`() {
        assertFalse(GoalHistoryCalc.reaches(78.0, 78.4))
        assertFalse(GoalHistoryCalc.reaches(78.0, 79.0))
    }

    @Test
    fun `a direcao nao conta`() {

        assertTrue(GoalHistoryCalc.reaches(65.0, 65.1))
        assertTrue(GoalHistoryCalc.reaches(75.0, 74.9))
    }

    @Test
    fun `pesagens anteriores ao objetivo nao o atingem`() {

        val pesagens = listOf(
            (hoje - 300) to 78.0,
            (hoje - 10) to 85.0,
        )
        assertNull(GoalHistoryCalc.firstDayReaching(78.0, setOnEpochDay = hoje - 20, weighIns = pesagens))
    }

    @Test
    fun `conta a pesagem do proprio dia em que o objetivo foi posto`() {
        val pesagens = listOf(hoje to 78.0)
        assertEquals(hoje, GoalHistoryCalc.firstDayReaching(78.0, setOnEpochDay = hoje, weighIns = pesagens))
    }

    @Test
    fun `escolhe o PRIMEIRO dia que atinge, nao o ultimo`() {
        val pesagens = listOf(
            (hoje - 5) to 78.1,
            (hoje - 2) to 77.9,
            hoje to 78.0,
        )
        assertEquals(
            hoje - 5,
            GoalHistoryCalc.firstDayReaching(78.0, setOnEpochDay = hoje - 40, weighIns = pesagens),
        )
    }

    @Test
    fun `sem pesagens nao ha nada atingido`() {
        assertNull(GoalHistoryCalc.firstDayReaching(78.0, hoje - 40, emptyList()))
    }

    @Test
    fun `carimba o objetivo atingido e calcula quanto demorou`() {
        val goal = GoalHistoryCalc.Goal(
            targetKg = 78.0,
            setOnEpochDay = hoje - 41,
            startWeightKg = 85.0,
        )
        val fechado = GoalHistoryCalc.settle(listOf(goal), listOf(hoje to 78.0)).single()
        assertEquals(hoje, fechado.reachedOnEpochDay)
        assertEquals(41L, fechado.daysTaken)
        assertEquals(7.0, fechado.distanceKg)
        assertTrue(fechado.reached)
    }

    @Test
    fun `um objetivo atingido nunca se reabre`() {

        val jaAtingido = GoalHistoryCalc.Goal(
            targetKg = 78.0,
            setOnEpochDay = hoje - 100,
            startWeightKg = 85.0,
            reachedOnEpochDay = hoje - 60,
        )
        val depois = GoalHistoryCalc.settle(listOf(jaAtingido), listOf(hoje to 84.0)).single()
        assertEquals(hoje - 60, depois.reachedOnEpochDay)
    }

    @Test
    fun `o que ainda nao foi atingido fica por fechar, sem inventar data`() {
        val goal = GoalHistoryCalc.Goal(78.0, hoje - 10, startWeightKg = 85.0)
        val depois = GoalHistoryCalc.settle(listOf(goal), listOf(hoje to 83.0)).single()
        assertNull(depois.reachedOnEpochDay)
        assertNull(depois.daysTaken)
        assertFalse(depois.reached)
    }

    @Test
    fun `sem peso de partida a app diz que nao sabe a distancia`() {
        val goal = GoalHistoryCalc.Goal(78.0, hoje, startWeightKg = null)
        assertNull(goal.distanceKg)
    }

    @Test
    fun `o primeiro objetivo guarda-se sempre`() {
        assertTrue(GoalHistoryCalc.shouldRecord(previousTargetKg = null, newTargetKg = 78.0))
    }

    @Test
    fun `regravar o mesmo numero nao cria entrada nova`() {

        assertFalse(GoalHistoryCalc.shouldRecord(78.0, 78.0))
        assertFalse(GoalHistoryCalc.shouldRecord(78.0, 78.04))
    }

    @Test
    fun `mudar o objetivo cria entrada nova`() {
        assertTrue(GoalHistoryCalc.shouldRecord(78.0, 77.0))
        assertTrue(GoalHistoryCalc.shouldRecord(78.0, 78.1))
    }

    @Test
    fun `apagar o objetivo nao cria entrada`() {
        assertFalse(GoalHistoryCalc.shouldRecord(78.0, null))
        assertFalse(GoalHistoryCalc.shouldRecord(null, null))
    }
}

class GoalChangeDetectionTest {

    @Test
    fun `mudancas de um decimo sao sempre detetadas`() {
        val casos = listOf(
            78.0 to 78.1,
            78.1 to 78.0,
            0.1 to 0.2,
            62.55 to 62.65,
            100.0 to 99.9,
            85.3 to 85.4,
        )
        for ((antes, depois) in casos) {
            assertTrue(
                GoalHistoryCalc.shouldRecord(antes, depois),
                "mudar de $antes para $depois é uma mudança e tem de ser registada",
            )
        }
    }

    @Test
    fun `diferencas abaixo de meio decimo continuam a ser o mesmo objetivo`() {
        assertFalse(GoalHistoryCalc.shouldRecord(78.0, 78.04))
        assertFalse(GoalHistoryCalc.shouldRecord(78.0, 77.96))
    }

    @Test
    fun `varrimento - nunca falha uma mudanca visivel ao decimo`() {

        var vistos = 0
        for (i in 400..1200) {
            val antes = i / 10.0
            for (delta in listOf(-3, -1, 1, 3)) {
                val depois = (i + delta) / 10.0
                assertTrue(
                    GoalHistoryCalc.shouldRecord(antes, depois),
                    "$antes → $depois não foi detetado",
                )
                vistos++
            }
        }
        assertTrue(vistos > 3000, "o varrimento encolheu: $vistos")
    }
}

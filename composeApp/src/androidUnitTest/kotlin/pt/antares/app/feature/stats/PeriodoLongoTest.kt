package pt.antares.app.feature.stats

import pt.antares.app.core.calc.StatsPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.nutrition.CoverageCalc
import pt.antares.app.core.nutrition.EfsaReference
import pt.antares.app.core.model.Sex
import pt.antares.app.testing.ViewModelHarness
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A cobertura de micronutrientes só tinha dia e semana. Um micronutriente não se lê num dia —
 * o fígado guarda vitamina A durante meses —, e era o período curto a dar alarmes falsos.
 *
 * Com o ano, a mesma consulta passa a atravessar mais de mil registos. O que este teste
 * guarda é que a soma continua a ser dividida pelos dias do período: sem isso, um ano de
 * comida bem feita compara-se com **uma** referência diária e dá 36 500% de cobertura.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PeriodoLongoTest : ViewModelHarness() {

    private val hoje = 20_000L

    private suspend fun anoDeRegistos() {
        // Cinco registos por dia durante um ano — mais do que a maioria das pessoas escreve,
        // e é esse o ponto: o período mais longo com a base cheia.
        for (dia in 0 until DIAS_DE_UM_ANO) {
            repeat(REGISTOS_POR_DIA) { i ->
                db.foodLogDao().upsert(
                    FoodLogEntity(
                        id = "log-$dia-$i",
                        epochDay = hoje - dia,
                        mealSlot = MealSlot.entries[i % MealSlot.entries.size],
                        foodId = "espinafres",
                        nameSnapshot = "Espinafres",
                        quantityGrams = 100.0,
                        kcalSnapshot = 23,
                        proteinSnapshot = 2.9,
                        carbsSnapshot = 3.6,
                        fatSnapshot = 0.4,
                        microsPer100Json = """{"iron_mg":2.7}""",
                        origin = LogOrigin.MANUAL,
                        eatenAtMin = null,
                        updatedAt = 0L,
                    ),
                )
            }
        }
    }

    @Test
    fun `um ano de registos e dividido pelos dias do ano`() = runTest(dispatcher) {
        anoDeRegistos()

        val totais = statsRepository().totals(hoje - (StatsPeriod.YEAR.dias - 1), hoje)

        // 5 registos de 100 g a 2,7 mg de ferro por 100 g = 13,5 mg por dia, 365 dias.
        val ferroTotal = FERRO_POR_REGISTO * REGISTOS_POR_DIA * DIAS_DE_UM_ANO
        assertTrue(
            abs((totais.byKey["iron_mg"] ?: 0.0) - ferroTotal) < TOLERANCIA,
            "o ano somou ${totais.byKey["iron_mg"]} mg de ferro em vez de $ferroTotal",
        )

        val cobertura = CoverageCalc.compute(
            totais,
            Sex.MALE,
            EfsaReference.parse(FERRO_DRV_CSV).all(),
            stage = null,
            days = StatsPeriod.YEAR.dias,
        ).single { it.key == "iron_mg" }

        // 13,5 mg por dia contra uma referência de 11 mg: 123%. Sem a divisão pelos dias
        // seriam 44 800%, e a barra dizia que estava tudo bem para sempre.
        assertEquals(123, cobertura.coveragePct)
    }

    @Test
    fun `cada periodo conta os dias que diz`() {
        assertEquals(1, StatsPeriod.DAY.dias)
        assertEquals(7, StatsPeriod.WEEK.dias)
        assertEquals(30, StatsPeriod.MONTH.dias)
        assertEquals(365, StatsPeriod.YEAR.dias)
    }

    private companion object {
        const val DIAS_DE_UM_ANO = 365
        const val REGISTOS_POR_DIA = 5
        const val FERRO_POR_REGISTO = 2.7
        const val TOLERANCIA = 0.01

        // Só a linha do ferro: o resto da tabela não muda nada nesta conta.
        const val FERRO_DRV_CSV = "key,male,female,unit\niron_mg,11,16,mg"
    }
}

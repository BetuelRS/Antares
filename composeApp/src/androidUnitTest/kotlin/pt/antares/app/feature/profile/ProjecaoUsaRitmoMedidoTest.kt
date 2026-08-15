package pt.antares.app.feature.profile

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.calc.NutritionCalc
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.Sex
import pt.antares.app.testing.Fabricas
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A data em que se chega ao peso-alvo sai do que **está a acontecer**, e não do que foi pedido.
 *
 * A regra das 7700 kcal por quilo continua a prescrever o défice — como heurística de prescrição
 * é universal e aceitável. Mas projetar com ela uma data assume que o gasto se mantém enquanto o
 * peso desce, e Hall et al. (2013, Int J Obes) mostraram que não se mantém: a data sai sempre
 * otimista, e o erro cresce com o tempo.
 *
 * O que impede isso é a projeção ler o ritmo medido das últimas quatro semanas e refazer-se a
 * cada pesagem. Trocar essa entrada pelo ritmo pedido no perfil desfazia a decisão sem partir
 * nada — é isso que estes testes apanham.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProjecaoUsaRitmoMedidoTest : ViewModelHarness() {

    private val hoje = 20_000L

    /** Uma pesagem por semana, a descer ao ritmo pedido, a acabar hoje. */
    private suspend fun historico(inicioKg: Double, kgPorSemana: Double, semanas: Int) {
        for (i in 0..semanas) {
            val dia = hoje - (semanas - i) * 7L
            db.weightLogDao().upsert(
                WeightLogEntity(
                    id = "w-$i",
                    epochDay = dia,
                    weightKg = inicioKg + kgPorSemana * i,
                    note = null,
                    updatedAt = dia,
                ),
            )
        }
    }

    /**
     * O ritmo pedido guarda-se em kcal por dia, que é a unidade em que entra na conta. É a
     * conversão pelas 7700 kcal por quilo — a mesma que este teste proíbe de projetar datas.
     */
    private suspend fun perfilComAlvo(alvoKg: Double, ritmoPedidoKgSemana: Double) {
        Fabricas.profileRepository(db, dispatcher).saveProfile(
            UserProfileEntity(
                sex = Sex.MALE,
                birthEpochDay = 10_000L,
                heightCm = 180,
                activityLevel = ActivityLevel.MODERATE,
                goalType = GoalType.LOSE,
                goalRateKcal = NutritionCalc.kcalPerDayFromWeeklyKg(ritmoPedidoKgSemana),
                macroStrategy = MacroStrategy.BALANCED,
                customProteinG = null,
                customCarbsG = null,
                customFatG = null,
                goalWeightKg = alvoKg,
                updatedAt = 0L,
            ),
        )
    }

    private suspend fun projecao() = Fabricas.healthProfileViewModel(db, dispatcher)
        .state.first { it.projection != null }
        .projection!!

    @Test
    fun `a data sai do ritmo medido e nao do ritmo pedido`() = runTest(dispatcher) {
        // Pedido: meio quilo por semana. Medido: um quilo por semana, o dobro.
        historico(inicioKg = 90.0, kgPorSemana = -1.0, semanas = 8)
        perfilComAlvo(alvoKg = 78.0, ritmoPedidoKgSemana = -0.5)
        advanceUntilIdle()

        val p = projecao()

        val semanas = assertNotNull(p.weeks)

        // Faltam 4 kg. Ao ritmo pedido seriam 8 semanas certas. Ao medido são 5: a média
        // exponencial atrasa-se de propósito, e por isso o intervalo em vez do número.
        assertTrue(
            semanas in 4..6,
            "a data tem de sair do que está a acontecer, e não das 8 semanas que o ritmo " +
                "pedido daria: projetar com ele é assumir que o gasto não desce com o peso. " +
                "Deu $semanas",
        )
    }

    @Test
    fun `quem esta a andar para o lado contrario recebe o aviso e nao uma data`() =
        runTest(dispatcher) {
            // Pediu perder, mas está a ganhar.
            historico(inicioKg = 80.0, kgPorSemana = 0.4, semanas = 8)
            perfilComAlvo(alvoKg = 75.0, ritmoPedidoKgSemana = -0.5)
            advanceUntilIdle()

            val p = projecao()

            assertTrue(p.movingAway, "com o ritmo pedido isto dava uma data, e seria mentira")
            assertEquals(null, p.etaEpochDay)
        }

    @Test
    fun `sem historico que chegue nao ha data nenhuma`() = runTest(dispatcher) {
        // Duas pesagens no mesmo dia não são um ritmo.
        historico(inicioKg = 90.0, kgPorSemana = -1.0, semanas = 0)
        perfilComAlvo(alvoKg = 78.0, ritmoPedidoKgSemana = -0.5)
        advanceUntilIdle()

        val p = projecao()

        assertEquals(
            null,
            p.weeks,
            "o ritmo pedido está lá e daria uma data à mesma; é essa a tentação a evitar",
        )
    }
}

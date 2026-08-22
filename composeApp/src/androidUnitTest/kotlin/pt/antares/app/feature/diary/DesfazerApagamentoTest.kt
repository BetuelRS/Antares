package pt.antares.app.feature.diary

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.MealSlot
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Não havia desfazer em lado nenhum: apagar um registo, uma pesagem ou uma série era
 * definitivo à primeira. E a base **apaga por marcação** desde sempre — o dado continuava
 * lá, e era só inalcançável.
 *
 * O que este teste guarda é a volta inteira: apagar, anular, e o registo voltar **com os
 * mesmos valores**. Um restauro que devolvesse a linha com outro número seria pior do que
 * não haver restauro.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DesfazerApagamentoTest : ViewModelHarness() {

    private val hoje = 20_000L

    private suspend fun alimento() = FoodEntity(
        id = "aveia",
        source = FoodSource.CUSTOM,
        sourceRef = null,
        namePt = "Aveia",
        nameEn = "Oats",
        brand = null,
        kcal = 389,
        proteinG = 16.9,
        carbsG = 66.3,
        sugarsG = null,
        fatG = 6.9,
        satFatG = null,
        microsJson = null,
        servingName = null,
        servingGrams = null,
        updatedAt = 0L,
    ).also { db.foodDao().upsert(it) }

    @Test
    fun `apagar e anular devolve o registo com os mesmos valores`() = runTest(dispatcher) {
        val repo = diaryRepository()
        repo.logFood(alimento(), quantityGrams = 80.0, slot = MealSlot.BREAKFAST, epochDay = hoje)

        val antes = assertNotNull(db.foodLogDao().dayLogs(hoje).singleOrNull())

        repo.delete(antes.id)
        assertTrue(db.foodLogDao().dayLogs(hoje).isEmpty(), "o apagamento não chegou a acontecer")

        repo.restore(antes.id)

        val depois = assertNotNull(
            db.foodLogDao().dayLogs(hoje).singleOrNull(),
            "desfazer não devolveu o registo",
        )
        assertEquals(antes.id, depois.id)
        assertEquals(antes.quantityGrams, depois.quantityGrams)
        assertEquals(antes.kcalSnapshot, depois.kcalSnapshot)
        assertEquals(antes.mealSlot, depois.mealSlot)
        assertEquals(antes.nameSnapshot, depois.nameSnapshot)
    }

    @Test
    fun `a pesagem apagada volta com o mesmo peso`() = runTest(dispatcher) {
        val repo = profileRepository()
        repo.upsertWeight(hoje, weightKg = 81.4, note = null)
        val antes = assertNotNull(db.weightLogDao().latest())

        repo.deleteWeight(antes.id)
        assertEquals(null, db.weightLogDao().latest(), "a pesagem continuou viva")

        repo.restoreWeight(antes.id)

        val depois = assertNotNull(db.weightLogDao().latest())
        assertEquals(81.4, depois.weightKg)
        assertEquals(hoje, depois.epochDay)
    }

    @Test
    fun `pesar outra vez no mesmo dia reaproveita a lapide, e desfazer nao a ressuscita`() =
        runTest(dispatcher) {
            // O índice único do dia conta as lápides — ver a decisão 0002. Uma pesagem nova
            // reaproveita a linha apagada em vez de colidir com ela, e por isso o `restore`
            // não pode fazer aparecer uma segunda pesagem do mesmo dia.
            val repo = profileRepository()
            repo.upsertWeight(hoje, weightKg = 81.4, note = null)
            val antiga = assertNotNull(db.weightLogDao().latest())

            repo.deleteWeight(antiga.id)
            repo.upsertWeight(hoje, weightKg = 79.0, note = null)

            repo.restoreWeight(antiga.id)

            val vivas = db.weightLogDao().exportRows().filter { it.epochDay == hoje }
            assertEquals(1, vivas.size, "ficaram duas pesagens no mesmo dia")
            assertEquals(79.0, vivas.single().weightKg, "o desfazer apagou a pesagem nova")
        }
}

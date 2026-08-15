package pt.antares.app.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import pt.antares.app.core.calc.WeeklyAggregate
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiRepositoryTest {

    private fun item(
        name: String = "peito de frango",
        grams: Double = 150.0,
        kcal: Int = 248,
        confidence: Double = 0.9,
        estimated: Boolean = false,
        micros: Map<String, Double>? = null,
    ) = AiFoodItem(
        name = name,
        matchedSource = "USDA",
        grams = grams,
        kcal = kcal,
        protein = 46.5,
        carbs = 0.0,
        fat = 5.4,
        micros = micros,
        confidence = confidence,
        estimated = estimated,
    )

    @Test
    fun `withGrams reescala tudo proporcionalmente`() {
        val doubled = item().withGrams(300.0)
        assertEquals(300.0, doubled.grams)
        assertEquals(496, doubled.kcal)
        assertEquals(93.0, doubled.protein)
        assertEquals(10.8, doubled.fat, 0.001)
    }

    @Test
    fun `withGrams ignora valores inválidos em vez de dividir por zero`() {
        assertEquals(150.0, item().withGrams(0.0).grams)
        assertEquals(150.0, item().withGrams(-5.0).grams)
    }

    @Test
    fun `needsReview marca confianca baixa e estimativas`() {
        assertTrue(!item().needsReview)
        assertTrue(item(confidence = 0.4).needsReview)

        assertTrue(item(estimated = true).needsReview)
    }

    @Test
    fun `usage conta o que resta`() {
        assertEquals(7, AiUsage(used = 3, limit = 10, trial = true).remaining)

        assertEquals(0, AiUsage(used = 12, limit = 10, trial = true).remaining)
    }

    @Test
    fun `confirmar grava uma linha por item com a origem certa`() = runTest {
        val dao = FakeLogSink()
        val repo = repo(dao)

        repo.confirmFood(
            items = listOf(item(name = "arroz", kcal = 200), item(name = "frango", kcal = 248)),
            mealSlot = MealSlot.LUNCH,
            epochDay = 20_000L,
            origin = LogOrigin.AI_PHOTO,
        )

        assertEquals(2, dao.rows.size)
        assertEquals(listOf("arroz", "frango"), dao.rows.map { it.nameSnapshot })
        assertTrue(dao.rows.all { it.origin == LogOrigin.AI_PHOTO })
        assertTrue(dao.rows.all { it.mealSlot == MealSlot.LUNCH && it.epochDay == 20_000L })

        assertTrue(dao.rows.all { it.foodId == null })

    }

    @Test
    fun `micros sao guardados por 100g e nao pelas gramas do item`() = runTest {
        val dao = FakeLogSink()
        val repo = repo(dao)

        repo.confirmFood(
            items = listOf(item(grams = 150.0, micros = mapOf("sodium" to 30.0))),
            mealSlot = MealSlot.DINNER,
            epochDay = 1L,
            origin = LogOrigin.AI_TEXT,
        )

        val stored = dao.rows.single().microsPer100Json
        assertTrue(stored!!.contains("20"), "esperava 20 mg/100g, veio: $stored")
    }

    @Test
    fun `sem micros nao inventa json`() = runTest {
        val dao = FakeLogSink()
        repo(dao).confirmFood(listOf(item()), MealSlot.SNACK, 1L, LogOrigin.AI_TEXT)
        assertNull(dao.rows.single().microsPer100Json)
    }

    private fun repo(sink: FakeLogSink) = AiRepository(
        client = NoAiClient,
        ensureAccount = {},
        saveFoodLog = { sink.rows += it },
        latestWeightKg = { 80.0 },
        persistUsage = { _, _ -> },
        io = Dispatchers.Unconfined,
        newId = { "id-${sink.rows.size}" },
        now = { 1_000L },
    )

    private class FakeLogSink {
        val rows = mutableListOf<FoodLogEntity>()
    }

    private object NoAiClient : AiClient {
        override suspend fun analyzeFoodText(text: String, lang: String, day: String) =
            error("não usado")

        override suspend fun analyzeFoodPhoto(imageBase64: String, mime: String, lang: String, day: String) =
            error("não usado")

        override suspend fun readLabel(imageBase64: String, mime: String, lang: String, day: String) =
            error("não usado")

        override suspend fun analyzeExercise(text: String, weightKg: Double, lang: String, day: String) =
            error("não usado")

    }
}

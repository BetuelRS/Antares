package pt.antares.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.FoodFtsEntity
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.util.TextNormalize
import pt.antares.app.feature.diary.DiaryRepository
import pt.antares.app.feature.fooddata.FoodRepository
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class FoodDaosTest {

    private lateinit var db: AntaresDb

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun repositorio() = FoodRepository(
        db.foodDao(),
        db.foodMarkDao(),
        db.foodNutrientDao(),
        db.searchMissDao(),
        Dispatchers.Default,
    )

    private fun food(
        id: String,
        namePt: String,
        nameEn: String = namePt,
        kcal: Int = 100,
        p: Double = 1.0,
        c: Double = 1.0,
        f: Double = 1.0,
    ) = FoodEntity(
        id = id, source = FoodSource.SEED, sourceRef = null, namePt = namePt, nameEn = nameEn,
        brand = null, kcal = kcal, proteinG = p, carbsG = c, sugarsG = null, fatG = f,
        satFatG = null, fiberG = null, sodiumMg = null, microsJson = null,
        servingName = null, servingGrams = null,
        verified = true, updatedAt = 1L,
    )

    private fun log(
        id: String,
        day: Long,
        slot: MealSlot,
        kcal: Int,
        p: Double,
        c: Double,
        f: Double,
        deleted: Boolean = false,
    ) = FoodLogEntity(
        id = id, epochDay = day, mealSlot = slot, foodId = null, nameSnapshot = "x",
        quantityGrams = 100.0, kcalSnapshot = kcal, proteinSnapshot = p, carbsSnapshot = c,
        fatSnapshot = f, microsPer100Json = null, origin = LogOrigin.MANUAL,
        updatedAt = 1L, deleted = deleted,
    )

    @Test
    fun `agregados do dia somam so o dia e ignoram apagados`() = runTest {
        val dao = db.foodLogDao()
        dao.upsert(log("1", 100, MealSlot.BREAKFAST, 200, 10.0, 20.0, 5.0))
        dao.upsert(log("2", 100, MealSlot.BREAKFAST, 150, 5.0, 30.0, 2.0))
        dao.upsert(log("3", 100, MealSlot.LUNCH, 300, 25.0, 10.0, 15.0))
        dao.upsert(log("4", 100, MealSlot.LUNCH, 100, 2.0, 15.0, 3.0))
        dao.upsert(log("5", 100, MealSlot.DINNER, 250, 20.0, 5.0, 12.0))
        dao.upsert(log("6", 100, MealSlot.SNACK, 80, 1.0, 18.0, 0.0))

        dao.upsert(log("7", 101, MealSlot.LUNCH, 999, 99.0, 99.0, 99.0))
        dao.upsert(log("8", 100, MealSlot.SNACK, 500, 50.0, 50.0, 50.0, deleted = true))

        val totals = dao.observeDayTotals(100).first()
        assertEquals(1080, totals.kcal)
        assertEquals(63.0, totals.proteinG, 1e-9)
        assertEquals(98.0, totals.carbsG, 1e-9)
        assertEquals(37.0, totals.fatG, 1e-9)
    }

    @Test
    fun `dia vazio devolve zeros`() = runTest {
        val totals = db.foodLogDao().observeDayTotals(42).first()
        assertEquals(0, totals.kcal)
        assertEquals(0.0, totals.proteinG, 1e-9)
    }

    @Test
    fun `editar o Food nao altera o log ja gravado`() = runTest {
        val diary = DiaryRepository(db.foodLogDao(), db.waterLogDao(), Dispatchers.Default)
        val apple = food("apple", "Maçã", kcal = 52, p = 0.3, c = 14.0, f = 0.2)
        db.foodDao().upsert(apple)

        diary.logFood(apple, quantityGrams = 200.0, slot = MealSlot.SNACK, epochDay = 10)
        val before = db.foodLogDao().observeDay(10).first().single()
        assertEquals(104, before.kcalSnapshot)
        assertEquals(0.6, before.proteinSnapshot, 1e-9)

        db.foodDao().upsert(apple.copy(kcal = 999, proteinG = 99.0, updatedAt = 2L))

        val after = db.foodLogDao().observeDay(10).first().single()
        assertEquals(104, after.kcalSnapshot)
        assertEquals(0.6, after.proteinSnapshot, 1e-9)
    }

    @Test
    fun `editar quantidade re-escala a partir do snapshot`() = runTest {
        val diary = DiaryRepository(db.foodLogDao(), db.waterLogDao(), Dispatchers.Default)
        val food = food("f", "Arroz", kcal = 130, p = 2.7, c = 28.0, f = 0.3)
        diary.logFood(food, quantityGrams = 100.0, slot = MealSlot.LUNCH, epochDay = 5)
        val logId = db.foodLogDao().observeDay(5).first().single().id

        diary.updateQuantity(logId, newGrams = 150.0)
        val updated = db.foodLogDao().observeDay(5).first().single()
        assertEquals(150.0, updated.quantityGrams, 1e-9)
        assertEquals(195, updated.kcalSnapshot)
    }

    private suspend fun insertSearchable(food: FoodEntity) {
        db.foodDao().upsertWithFts(
            food,
            TextNormalize.normalize("${food.namePt} ${food.nameEn} ${food.brand.orEmpty()}"),
        )
    }

    @Test
    fun `reimportar nao duplica o alimento na pesquisa`() = runTest {
        val repo = repositorio()
        val f = food("1", "Pão de centeio")
        insertSearchable(f)

        db.foodDao().insertAll(listOf(f))
        db.foodDao().deleteFtsIn(listOf(f.id))
        db.foodDao().insertFtsAll(
            listOf(FoodFtsEntity(foodId = f.id, searchText = TextNormalize.normalize(f.namePt))),
        )

        assertEquals(1, repo.search("pao").size, "o alimento não pode sair duas vezes")
    }

    @Test
    fun `pesquisa sem acentos encontra com acentos`() = runTest {
        val repo = repositorio()
        insertSearchable(food("1", "Pão de centeio"))
        insertSearchable(food("2", "Maçã reineta"))

        assertEquals(1, repo.search("pao").size)
        assertEquals("Pão de centeio", repo.search("pao").single().namePt)
        assertEquals("Maçã reineta", repo.search("maca").single().namePt)
    }

    @Test
    fun `ranking favorito antes de recente antes de alfabetico`() = runTest {
        val repo = repositorio()
        insertSearchable(food("b", "Arroz basmati"))
        insertSearchable(food("i", "Arroz integral"))
        insertSearchable(food("w", "Arroz branco"))

        // As marcas vivem à parte desde a v27: a ordem da pesquisa junta-as ao alimento,
        // em vez de as ler da linha dele.
        repo.touchLastUsed("i")
        repo.toggleFavorite("w")

        val names = repo.search("arroz").map { it.namePt }
        assertEquals(listOf("Arroz branco", "Arroz integral", "Arroz basmati"), names)
    }

    @Test
    fun `curados PT aparecem antes dos USDA na pesquisa`() = runTest {
        val repo = repositorio()

        insertSearchable(food("usda-1", "Beverages, cafe substitute, cereal grain, powder"))
        insertSearchable(food("ptx_cafe_leite", "Café com leite"))

        val names = repo.search("cafe").map { it.namePt }

        assertEquals("Café com leite", names.first())
    }

    @Test
    fun `barcode cacheado resolve sem rede no segundo scan`() = runTest {
        val repo = repositorio()

        val product = food("off_560123", "Água com gás").copy(
            source = pt.antares.app.core.model.FoodSource.OFF,
            sourceRef = "560123",
        )
        repo.cacheOnline(product)

        val cached = repo.byBarcode("560123")
        assertEquals("off_560123", cached?.id)
        assertEquals("Água com gás", cached?.namePt)

        assertEquals(null, repo.byBarcode("000000"))
    }

    @Test
    fun `pesquisa FTS mantem-se na ordem dos milissegundos no catalogo grande`() = runTest {
        val categorias = listOf("arroz", "frango", "batata", "maca", "leite", "pao", "atum", "queijo")
        val foods = (0 until 4000).map { i ->
            food("id$i", "${categorias[i % categorias.size]} tipo $i", kcal = 100 + i % 300)
        }
        val fts = foods.map {
            FoodFtsEntity(foodId = it.id, searchText = TextNormalize.normalize(it.namePt))
        }
        foods.chunked(400).forEach { db.foodDao().insertAll(it) }
        fts.chunked(400).forEach { db.foodDao().insertFtsAll(it) }

        val repo = repositorio()

        repeat(5) { repo.search(categorias[it % categorias.size]) }

        val runs = 21
        val samples = (0 until runs).map { i ->
            val start = System.nanoTime()
            repo.search(categorias[i % categorias.size])
            (System.nanoTime() - start) / 1_000_000.0
        }.sorted()
        val medianMs = samples[runs / 2]

        println("FTS mediana sobre ${foods.size} alimentos: ${medianMs}ms (pior: ${samples.last()}ms)")
        assertTrue(medianMs < 150.0, "FTS demasiado lenta: mediana ${medianMs}ms — o índice está a ser ignorado?")
    }

    @Test
    fun `touchLastUsed guarda a quantidade usada`() = runTest {
        db.foodDao().insertAll(listOf(food("f1", "Arroz")))
        val repo = repositorio()

        repo.touchLastUsed("f1", amountG = 85.0)

        assertEquals(85.0, db.foodMarkDao().byFoodId("f1")?.lastAmountG)
    }

    @Test
    fun `touchLastUsed sem quantidade NAO apaga a que la estava`() = runTest {
        db.foodDao().insertAll(listOf(food("f1", "Arroz")))
        val repo = repositorio()
        repo.touchLastUsed("f1", amountG = 85.0)

        repo.touchLastUsed("f1")

        assertEquals(85.0, db.foodMarkDao().byFoodId("f1")?.lastAmountG)
    }

    @Test
    fun `registar outra vez substitui a quantidade anterior`() = runTest {
        db.foodDao().insertAll(listOf(food("f1", "Arroz")))
        val repo = repositorio()

        repo.touchLastUsed("f1", amountG = 85.0)
        repo.touchLastUsed("f1", amountG = 120.0)

        assertEquals(120.0, db.foodMarkDao().byFoodId("f1")?.lastAmountG)
    }

    @Test
    fun `alimento nunca registado nao tem marca nenhuma`() = runTest {
        db.foodDao().insertAll(listOf(food("f1", "Arroz")))

        // Sem marca, e não com uma marca vazia: é isso que faz a tabela ter dezenas de
        // linhas num catálogo de oito mil alimentos.
        assertEquals(null, db.foodMarkDao().byFoodId("f1"))
    }
}

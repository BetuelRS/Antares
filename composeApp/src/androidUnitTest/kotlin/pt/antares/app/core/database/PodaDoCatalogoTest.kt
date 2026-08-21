package pt.antares.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.database.entities.MealTemplateItemEntity
import pt.antares.app.core.database.entities.RecipeIngredientEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * A poda do catálogo antigo, e as seis maneiras de um alimento estar a ser usado.
 *
 * Desde a 2.4.0 o catálogo é substituído por inteiro a cada versão: escreve-se o que veio
 * e apaga-se o que deixou de vir. **É a operação mais perigosa da app** — uma poda a mais
 * não rebenta nada, não dá erro e não aparece em lado nenhum: a pessoa é que, um dia,
 * abre uma receita e falta-lhe um ingrediente.
 *
 * Por isso cada uma das seis condições tem aqui um teste, e não um teste para todas. Uma
 * condição que caia da consulta continuaria a passar num teste que só verificasse o caso
 * comum.
 *
 * As receitas e as refeições-tipo **não estavam na consulta antiga**, e foi este ficheiro
 * que as pôs lá.
 */
@RunWith(RobolectricTestRunner::class)
class PodaDoCatalogoTest {

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

    /** O instante desta instalação. Tudo o que ficou para trás dele é da versão anterior. */
    private val agora = 1_000L
    private val antes = 500L

    private fun alimento(
        id: String,
        quando: Long = antes,
        source: FoodSource = FoodSource.SEED,
        favorito: Boolean = false,
        usadoEm: Long = 0L,
        porcao: Double? = null,
    ) = FoodEntity(
        id = id, source = source, sourceRef = null, namePt = id, nameEn = id,
        brand = null, kcal = 100, proteinG = 1.0, carbsG = 1.0, sugarsG = null, fatG = 1.0,
        satFatG = null, fiberG = null, sodiumMg = null, microsJson = null,
        servingName = null, servingGrams = null, isFavorite = favorito, lastUsedAt = usadoEm,
        lastAmountG = porcao, verified = true, updatedAt = quando,
    )

    private suspend fun podar() = db.foodDao().podarCatalogoAnterior(agora)

    @Test
    fun `o que a versao nova nao trouxe e apagado`() = runTest {
        db.foodDao().upsert(alimento("ciqual-velho"))
        db.foodDao().upsert(alimento("ciqual-novo", quando = agora))

        assertEquals(1, podar())
        assertNull(db.foodDao().byId("ciqual-velho"))
        assertNotNull(db.foodDao().byId("ciqual-novo"))
    }

    @Test
    fun `um favorito nunca e apagado`() = runTest {
        db.foodDao().upsert(alimento("ciqual-favorito", favorito = true))
        assertEquals(0, podar())
        assertNotNull(db.foodDao().byId("ciqual-favorito"))
    }

    @Test
    fun `um alimento usado nunca e apagado`() = runTest {
        db.foodDao().upsert(alimento("ciqual-usado", usadoEm = 42L))
        assertEquals(0, podar())
        assertNotNull(db.foodDao().byId("ciqual-usado"))
    }

    @Test
    fun `um alimento com porcao guardada nunca e apagado`() = runTest {
        db.foodDao().upsert(alimento("ciqual-porcao", porcao = 80.0))
        assertEquals(0, podar())
        assertNotNull(db.foodDao().byId("ciqual-porcao"))
    }

    @Test
    fun `um alimento num registo do diario nunca e apagado`() = runTest {
        db.foodDao().upsert(alimento("ciqual-registado"))
        db.foodLogDao().upsert(
            FoodLogEntity(
                id = "l1", epochDay = 1, mealSlot = MealSlot.LUNCH, foodId = "ciqual-registado",
                nameSnapshot = "x", quantityGrams = 100.0, kcalSnapshot = 100,
                proteinSnapshot = 1.0, carbsSnapshot = 1.0, fatSnapshot = 1.0,
                microsPer100Json = null, origin = LogOrigin.MANUAL, updatedAt = 1L,
            ),
        )
        assertEquals(0, podar())
        assertNotNull(db.foodDao().byId("ciqual-registado"))
    }

    @Test
    fun `um ingrediente de receita nunca e apagado`() = runTest {

        // Este é o caso que a consulta antiga não cobria. Uma receita aponta para o
        // alimento em vez de lhe copiar a nutrição, e por isso perder a linha é perder o
        // ingrediente — sem erro nenhum, e sem nada no ecrã a dizer que faltava.
        db.foodDao().upsert(alimento("ciqual-ingrediente"))
        db.recipeIngredientDao().upsert(
            RecipeIngredientEntity(
                id = "i1", recipeId = "r1", foodId = "ciqual-ingrediente",
                grams = 50.0, updatedAt = 1L,
            ),
        )
        assertEquals(0, podar())
        assertNotNull(db.foodDao().byId("ciqual-ingrediente"))
    }

    @Test
    fun `um item de refeicao-tipo nunca e apagado`() = runTest {
        db.foodDao().upsert(alimento("ciqual-refeicao"))
        db.mealTemplateItemDao().upsert(
            MealTemplateItemEntity(
                id = "m1", templateId = "t1", foodId = "ciqual-refeicao", nameSnapshot = "x",
                quantityGrams = 100.0, kcalSnapshot = 100, proteinSnapshot = 1.0,
                carbsSnapshot = 1.0, fatSnapshot = 1.0, microsPer100Json = null, updatedAt = 1L,
            ),
        )
        assertEquals(0, podar())
        assertNotNull(db.foodDao().byId("ciqual-refeicao"))
    }

    @Test
    fun `o que a pessoa criou ou trouxe de fora nao e catalogo`() = runTest {

        // A poda decide-se pela origem e não pelo identificador. Um alimento criado à mão,
        // vindo da Open Food Facts ou estimado por AI não vem no ficheiro, e por isso
        // estaria sempre por reescrever — se a consulta olhasse só ao `updatedAt`,
        // desaparecia na primeira actualização.
        db.foodDao().upsert(alimento("meu-1", source = FoodSource.CUSTOM))
        db.foodDao().upsert(alimento("off-1", source = FoodSource.OFF))
        db.foodDao().upsert(alimento("ai-1", source = FoodSource.AI_ESTIMATE))

        assertEquals(0, podar())
        assertNotNull(db.foodDao().byId("meu-1"))
        assertNotNull(db.foodDao().byId("off-1"))
        assertNotNull(db.foodDao().byId("ai-1"))
    }
}

package pt.antares.app.feature.diary

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.util.MINUTES_PER_DAY
import pt.antares.app.core.util.currentMinuteOfDay
import pt.antares.app.core.util.todayEpochDay
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A hora a que se comeu é diferente da hora a que se registou, e é a primeira que interessa:
 * é dela que sai a janela alimentar e o cruzamento com o jejum. Registar o jantar na manhã
 * seguinte tem de continuar a ser o jantar.
 *
 * O que este teste fixa é **quando a app se cala**: num dia que não é hoje ela não sabe a
 * que horas se comeu, e pôr lá a hora de agora encheria a janela alimentar com o momento em
 * que a pessoa se lembrou de registar.
 */
@RunWith(RobolectricTestRunner::class)
class HoraDaRefeicaoTest {

    private val db: AntaresDb = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AntaresDb::class.java,
    ).setQueryCoroutineContext(Dispatchers.Default).build()

    private val repo = DiaryRepository(db.foodLogDao(), db.waterLogDao(), Dispatchers.Default)

    @AfterTest
    fun tearDown() = db.close()

    private val aveia = FoodEntity(
        id = "f1",
        source = FoodSource.SEED,
        sourceRef = null,
        namePt = "Aveia",
        nameEn = "Oats",
        brand = null,
        kcal = 380,
        proteinG = 13.0,
        carbsG = 60.0,
        fatG = 7.0,
        sugarsG = null,
        satFatG = null,
        fiberG = null,
        sodiumMg = null,
        microsJson = null,
        servingName = null,
        servingGrams = null,
        updatedAt = 1_000,
    )

    @Test
    fun `registar hoje guarda a hora a que se comeu`() = runTest {
        val antes = currentMinuteOfDay()
        repo.logFood(aveia, quantityGrams = 50.0, slot = MealSlot.BREAKFAST, epochDay = todayEpochDay())
        val depois = currentMinuteOfDay()

        val hora = assertNotNull(
            db.foodLogDao().exportRows().single().eatenAtMin,
            "o registo de hoje ficou sem hora, e sem ela não há janela alimentar",
        )
        assertTrue(hora in antes..depois, "a hora $hora não é a de agora ($antes a $depois)")
        assertTrue(hora in 0 until MINUTES_PER_DAY, "minutos fora do dia: $hora")
    }

    @Test
    fun `registar num dia passado fica sem hora, em vez de inventar uma`() = runTest {
        repo.logFood(
            aveia,
            quantityGrams = 50.0,
            slot = MealSlot.DINNER,
            epochDay = todayEpochDay() - 1,
        )

        assertNull(
            db.foodLogDao().exportRows().single().eatenAtMin,
            "pôs a hora de agora num registo de ontem — a janela alimentar passaria a medir " +
                "quando a pessoa se lembrou de registar, e não quando comeu",
        )
    }

    @Test
    fun `corrigir a hora de um registo antigo enche o buraco`() = runTest {
        repo.logFood(aveia, quantityGrams = 50.0, slot = MealSlot.DINNER, epochDay = todayEpochDay() - 3)
        val id = db.foodLogDao().exportRows().single().id

        repo.updateEatenAt(id, 20 * 60 + 30)

        assertEquals(
            1230,
            db.foodLogDao().exportRows().single().eatenAtMin,
            "não deu para pôr a hora à mão num registo que nasceu sem ela",
        )
    }

    @Test
    fun `apagar a hora deixa o registo sem hora, e nao a zero`() = runTest {
        repo.logFood(aveia, quantityGrams = 50.0, slot = MealSlot.LUNCH, epochDay = todayEpochDay())
        val id = db.foodLogDao().exportRows().single().id

        repo.updateEatenAt(id, null)

        assertNull(
            db.foodLogDao().exportRows().single().eatenAtMin,
            "apagar a hora pôs meia-noite — que é uma hora, e das piores para a janela",
        )
    }

    @Test
    fun `uma hora fora do dia e recusada, em vez de ficar gravada`() = runTest {
        repo.logFood(aveia, quantityGrams = 50.0, slot = MealSlot.LUNCH, epochDay = todayEpochDay())
        val id = db.foodLogDao().exportRows().single().id

        assertFailsWith<IllegalArgumentException> { repo.updateEatenAt(id, MINUTES_PER_DAY) }
        assertFailsWith<IllegalArgumentException> { repo.updateEatenAt(id, -1) }
    }

    @Test
    fun `as calorias rapidas seguem a mesma regra`() = runTest {
        repo.logQuickCalories(300, "bolo", MealSlot.SNACK, todayEpochDay())
        repo.logQuickCalories(300, "bolo de ontem", MealSlot.SNACK, todayEpochDay() - 1)

        val porDia = db.foodLogDao().exportRows().associateBy { it.epochDay }
        assertNotNull(porDia.getValue(todayEpochDay()).eatenAtMin)
        assertNull(porDia.getValue(todayEpochDay() - 1).eatenAtMin)
    }
}

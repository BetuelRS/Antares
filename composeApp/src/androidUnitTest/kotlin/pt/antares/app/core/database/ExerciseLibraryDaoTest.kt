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
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.feature.workout.data.ExerciseSeeder
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ExerciseLibraryDaoTest {

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

    private fun ex(
        id: String,
        nameEn: String,
        primary: List<String>,
        equipment: String,
        level: String,
    ) = ExerciseEntity(
        id = id,
        nameEn = nameEn,
        namePt = nameEn,
        searchText = nameEn.lowercase(),
        category = "strength",
        force = "push",
        mechanic = "compound",
        equipment = equipment,
        level = level,
        primaryMuscles = ExerciseSeeder.wrap(primary),
        secondaryMuscles = "",
        instructionsEnJson = "[]",
        instructionsPtJson = "[]",
        imagesJson = "[]",
        updatedAt = 1L,
    )

    @Test
    fun `filtro combina nome, musculo, equipamento e so os meus`() = runTest {
        val dao = db.exerciseLibraryDao()
        dao.upsertAll(
            listOf(
                ex("1", "bench press", listOf("chest"), "barbell", "beginner"),
                ex("2", "squat", listOf("quadriceps"), "barbell", "intermediate"),
                ex("3", "cable fly", listOf("chest"), "cable", "beginner"),
            ),
        )
        dao.upsert(ex("4", "o meu", listOf("chest"), "barbell", "beginner").copy(isCustom = true))

        assertEquals(3, dao.observeFiltered("", "chest", null, false).first().size)

        assertEquals(
            listOf("1", "4"),
            dao.observeFiltered("", "chest", "barbell", false).first().map { it.id }.sorted(),
        )

        assertEquals(listOf("2"), dao.observeFiltered("squat", null, null, false).first().map { it.id })

        // O filtro de nível saiu na 2.27.0 e no lugar dele entrou o «só os meus»: o nível é
        // uma classificação da base de origem, e ninguém procura por ela.
        assertEquals(listOf("4"), dao.observeFiltered("", null, null, true).first().map { it.id })
    }

    @Test
    fun `soft delete de custom esconde e ignora seed`() = runTest {
        val dao = db.exerciseLibraryDao()
        dao.upsert(ex("seed", "seeded", listOf("chest"), "barbell", "beginner"))
        dao.upsert(ex("mine", "custom", listOf("chest"), "barbell", "beginner").copy(isCustom = true))

        dao.softDeleteCustom("mine", now = 10)
        dao.softDeleteCustom("seed", now = 10)

        val ids = dao.observeAll().first().map { it.id }
        assertEquals(listOf("seed"), ids)
    }

    @Test
    fun `correcao de nomes poe ingles limpo e curados PT poupando custom`() = runTest {
        val dao = db.exerciseLibraryDao()

        dao.upsert(ex("s1", "Barbell Squat", listOf("quadriceps"), "barbell", "beginner").copy(namePt = "Barra Agachamento"))
        dao.upsert(ex("s2", "Car Deadlift", listOf("hamstrings"), "barbell", "expert").copy(namePt = "Car Levantamento Terra"))
        dao.upsert(ex("mine", "Meu Exercício", listOf("chest"), "barbell", "beginner").copy(isCustom = true, namePt = "Meu Exercício"))

        val changed = dao.resetSeedNamesToEnglish()
        assertEquals(2, changed)
        assertEquals("Barbell Squat", dao.byId("s1")?.namePt)
        assertEquals("Car Deadlift", dao.byId("s2")?.namePt)
        assertEquals("Meu Exercício", dao.byId("mine")?.namePt)

        dao.setNamePtByNameEn("Barbell Squat", "Agachamento com Barra")
        assertEquals("Agachamento com Barra", dao.byId("s1")?.namePt)
    }

    @Test
    fun `wrap e unwrap sao inversos`() {
        assertEquals("|chest|triceps|", ExerciseSeeder.wrap(listOf("chest", "triceps")))
        assertEquals("", ExerciseSeeder.wrap(emptyList()))
        assertEquals(listOf("chest", "triceps"), ExerciseSeeder.unwrap("|chest|triceps|"))
        assertEquals(emptyList(), ExerciseSeeder.unwrap(""))
    }
}

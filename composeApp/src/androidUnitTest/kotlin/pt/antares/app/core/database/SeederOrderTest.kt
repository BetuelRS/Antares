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
import pt.antares.app.feature.workout.data.ExerciseSeeder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SeederOrderTest {

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

    @Test
    fun `com a flag posta nao le o seed e mantem o imageBaseUrl`() = runTest {
        val base = "https://exemplo.invalido/imagens/"
        db.dbInfoDao().upsert(DbInfo("seed_exercises_imported", "v1"))
        db.dbInfoDao().upsert(DbInfo("seed_exercises_image_base", base))

        val seeder = ExerciseSeeder(db, Dispatchers.Default)
        seeder.seedIfNeeded()

        assertEquals(base, seeder.imageBaseUrl, "o imageBaseUrl não veio do db_info")
        assertEquals(0, db.exerciseLibraryDao().count(), "importou apesar da flag estar posta")
    }

    @Test
    fun `sem imageBaseUrl guardado cai no valor por omissao`() = runTest {

        db.dbInfoDao().upsert(DbInfo("seed_exercises_imported", "v1"))

        val seeder = ExerciseSeeder(db, Dispatchers.Default)
        seeder.seedIfNeeded()

        assertTrue(seeder.imageBaseUrl.startsWith("https://"), "ficou sem base de imagens")
    }

    @Test
    fun `nenhum seeder le o recurso antes de verificar a flag`() {
        val dir = File("src/commonMain/kotlin/pt/antares/app")
        val ofensores = dir.walkTopDown()
            .filter { it.name.endsWith("Seeder.kt") }
            .mapNotNull { f ->
                val corpo = f.readText().substringAfter("fun seedIfNeeded", "")
                if (corpo.isBlank()) return@mapNotNull null
                val leitura = corpo.indexOf("Res.readBytes")
                val verificacao = corpo.indexOf("dbInfoDao()")
                    .let { if (it >= 0) it else corpo.indexOf("count") }

                if (leitura < 0) return@mapNotNull null
                if (verificacao in 0 until leitura) null else f.name
            }
            .toList()

        assertEquals(
            emptyList(),
            ofensores,
            "estes seeders leem o ficheiro do disco antes de saber se precisam dele — " +
                "custa em todos os arranques da app",
        )
    }

    @Test
    fun `a leitura do seed esta protegida`() {

        val seeders = File("src/commonMain/kotlin/pt/antares/app").walkTopDown()
            .filter { it.name.endsWith("Seeder.kt") }
            .filter { it.readText().contains("Res.readBytes") }
            .toList()
        assertTrue(seeders.isNotEmpty(), "o teste perdeu o alvo")

        val desprotegidos = seeders
            .filterNot { f ->
                Regex("""runCatching\s*\{\s*Res\.readBytes""").containsMatchIn(f.readText())
            }
            .map { it.name }

        assertEquals(
            emptyList(),
            desprotegidos,
            "leitura de recurso sem runCatching dentro de um launch sem handler",
        )
    }
}

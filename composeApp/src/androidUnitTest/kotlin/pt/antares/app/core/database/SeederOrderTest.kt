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
import pt.antares.app.core.crash.NoCrashStore
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

        val seeder = ExerciseSeeder(db, Dispatchers.Default, NoCrashStore)
        seeder.seedIfNeeded()

        assertEquals(base, seeder.imageBaseUrl, "o imageBaseUrl não veio do db_info")
        assertEquals(0, db.exerciseLibraryDao().count(), "importou apesar da flag estar posta")
    }

    @Test
    fun `sem imageBaseUrl guardado cai no valor por omissao`() = runTest {

        db.dbInfoDao().upsert(DbInfo("seed_exercises_imported", "v1"))

        val seeder = ExerciseSeeder(db, Dispatchers.Default, NoCrashStore)
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

        // A forma não interessa — `runCatching` ou `try` servem os dois. O que tem de ser
        // verdade é que a exceção seja apanhada: isto corre num `launch` sem tratador, e
        // sem proteção uma leitura falhada mata o arranque em vez de deixar a app sem
        // catálogo. Quando é `try`, o `SeedFalhadoDeixaRastoTest` exige o rasto por cima.
        val desprotegidos = seeders
            .flatMap { f ->
                val texto = f.readText()
                val linhas = texto.lines()

                // A proteção também pode estar uma chamada acima, num ajudante do próprio
                // semeador — foi para lá que ela foi quando passou a haver duas origens
                // para o catálogo, a do APK e a que desce da rede. Nesse caso exige-se o
                // `catch` no mesmo ficheiro: o que não se aceita é a leitura ficar a
                // descoberto, não a forma como está tapada.
                val ajudante = Regex("""\bcatch\s*\(""").containsMatchIn(texto)

                linhas.withIndex()
                    .filter { (_, l) -> l.contains("Res.readBytes") }
                    .filterNot { (i, _) ->
                        linhas.subList(maxOf(0, i - RECUO_LINHAS), i + 1).any {
                            it.contains("runCatching") ||
                                Regex("""\btry\s*\{""").containsMatchIn(it) ||
                                (ajudante && it.contains("tentar("))
                        }
                    }
                    .map { (i, _) -> "${f.name}:${i + 1}" }
            }

        assertEquals(
            emptyList(),
            desprotegidos,
            "leitura de recurso sem nada a apanhar a exceção, dentro de um launch sem tratador",
        )
    }

    private companion object {
        // Quantas linhas acima da leitura se procura a proteção. Chega para o abre-chaveta
        // e a anotação de opt-in ficarem entre os dois.
        const val RECUO_LINHAS = 12
    }
}

package pt.antares.app.core.privacy

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.di.coreModule
import pt.antares.app.core.di.databaseModule
import pt.antares.app.core.di.viewModelModule
import pt.antares.app.core.database.entities.WeightLogEntity
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * O caminho de volta de uma cópia, montado como na app: o mesmo `DataExporter` e o mesmo
 * `BackupImporter` que o Koin entrega aos ecrãs, com as 26 tabelas ligadas.
 *
 * Não passa pela interface porque o que a lança é o selecionador de ficheiros do sistema,
 * que um teste não abre. Tudo o que vem a seguir a esse toque está aqui.
 *
 * O `BackupImporterTest` cobre as regras de junção com fontes de mentira, e o
 * `BackupImportRollbackTest` cobre a falha a meio. Este cobre o que nenhum dos dois vê: que
 * a lista de fontes ligada no `CoreModule` leva mesmo os dados lá e de volta.
 */
@RunWith(RobolectricTestRunner::class)
class RestaurarBackupTest : KoinTest {

    private lateinit var emMemoria: AntaresDb

    @BeforeTest
    fun arranca() {
        stopKoin()
        emMemoria = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AntaresDb::class.java,
        ).setQueryCoroutineContext(Dispatchers.Default).build()

        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            // A base verdadeira do `databaseModule` usa o SQLite empacotado, que é código
            // nativo e não existe num teste de JVM. Troca-se por uma em memória; tudo o
            // resto do grafo — as 26 fontes de exportação incluídas — fica como na app.
            modules(coreModule, databaseModule, viewModelModule, module { single { emMemoria } })
        }
    }

    @AfterTest
    fun fecha() {
        stopKoin()
        emMemoria.close()
    }

    private fun peso(id: String, dia: Long, kg: Double) = WeightLogEntity(
        id = id, epochDay = dia, weightKg = kg, note = null,
        updatedAt = dia, deleted = false, dirty = false,
    )

    @Test
    fun `o que sai numa copia volta a entrar pelo substituir`() = runTest {
        val db = emMemoria
        db.weightLogDao().upsert(peso("a", dia = 20_000, kg = 80.0))
        db.weightLogDao().upsert(peso("b", dia = 20_001, kg = 79.5))

        val ficheiro = get<DataExporter>().exportJson()

        // A pessoa mexe no telemóvel depois de fazer a cópia: apaga uma pesagem e muda a
        // outra. É este estado que o `substituir` tem de desfazer.
        db.weightLogDao().upsert(peso("a", dia = 20_000, kg = 99.9))
        db.weightLogDao().softDelete("b", now = 20_002)

        val r = get<BackupImporter>().import(ficheiro, ImportMode.REPLACE)

        assertIs<ImportResult.Done>(r, "esperava Done, veio $r")
        val pesagens = db.weightLogDao().exportRows().associateBy { it.id }
        assertEquals(2, pesagens.size, "o `substituir` não repôs as duas pesagens da cópia")
        assertEquals(80.0, pesagens.getValue("a").weightKg, "não desfez a alteração feita depois")
        assertEquals(79.5, pesagens.getValue("b").weightKg, "não trouxe de volta a que foi apagada")
    }

    @Test
    fun `juntar mantem o que ficou mais recente no telemovel`() = runTest {
        val db = emMemoria
        db.weightLogDao().upsert(peso("a", dia = 20_000, kg = 80.0))
        val ficheiro = get<DataExporter>().exportJson()

        // Mais recente por `updatedAt`, que é o critério do desempate.
        db.weightLogDao().upsert(
            peso("a", dia = 20_000, kg = 78.0).copy(updatedAt = 30_000),
        )

        val r = get<BackupImporter>().import(ficheiro, ImportMode.MERGE)

        assertIs<ImportResult.Done>(r)
        assertEquals(
            78.0,
            db.weightLogDao().exportRows().single().weightKg,
            "o `juntar` sobrepôs-se ao que estava no telemóvel e era mais recente",
        )
    }

    @Test
    fun `um ficheiro que nao e uma copia nao chega a tocar na base`() = runTest {
        val db = emMemoria
        db.weightLogDao().upsert(peso("a", dia = 20_000, kg = 80.0))

        val r = get<BackupImporter>().import("""{"tabelas":[]}""", ImportMode.REPLACE)

        assertIs<ImportResult.NotABackup>(r)
        assertTrue(
            db.weightLogDao().exportRows().isNotEmpty(),
            "recusou o ficheiro depois de já ter esvaziado a base",
        )
    }
}

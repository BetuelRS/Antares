package pt.antares.app.core.privacy

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.GoalHistoryEntity
import pt.antares.app.core.database.entities.WaterLogEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Restaurar uma cópia é o caminho mais perigoso da app: o `substituir` esvazia as tabelas
 * antes de as reescrever, e a única cópia dos dados é o ficheiro que se está a ler. Se uma
 * escrita falhar a meio, o telemóvel não pode ficar sem os dados velhos e com metade dos
 * novos.
 *
 * Ao contrário do `BackupImporterTest`, este corre contra uma base verdadeira — é a única
 * forma de provar que a transação desfaz mesmo.
 */
@RunWith(RobolectricTestRunner::class)
class BackupImportRollbackTest {

    private val db: AntaresDb = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AntaresDb::class.java,
    ).setQueryCoroutineContext(Dispatchers.Default).build()

    @AfterTest
    fun tearDown() = db.close()

    private fun peso(id: String, kg: Double, dia: Long) = WeightLogEntity(
        id = id, epochDay = dia, weightKg = kg, note = null,
        updatedAt = 1_000, deleted = false,
    )

    private fun agua(id: String, dia: Long) =
        WaterLogEntity(id = id, epochDay = dia, ml = 500, updatedAt = 1_000)

    private fun objetivo(id: String) = GoalHistoryEntity(
        id = id, targetKg = 75.0, setOnEpochDay = 20_000, updatedAt = 1_000,
    )

    /**
     * As três tabelas do teste, pela ordem em que a importação lhes toca. A terceira falha
     * a escrever, no papel de disco cheio ou de restrição violada.
     */
    private fun fontes(terceiraFalha: Boolean) = listOf(
        ExportSource(
            "weight_log",
            WeightLogEntity.serializer(),
            restore = { linhas -> linhas.forEach { db.weightLogDao().upsert(it) } },
        ) { db.weightLogDao().exportRows() },
        ExportSource(
            "water_log",
            WaterLogEntity.serializer(),
            restore = { linhas -> linhas.forEach { db.waterLogDao().upsert(it) } },
        ) { db.waterLogDao().exportRows() },
        ExportSource(
            "goal_history",
            GoalHistoryEntity.serializer(),
            restore = { linhas ->
                if (terceiraFalha) error("a base falhou a meio da terceira tabela")
                linhas.forEach { db.goalHistoryDao().upsert(it) }
            },
        ) { db.goalHistoryDao().exportRows() },
    )

    private suspend fun ficheiroDeBackup(): String = DataExporter(
        sources = listOf(
            ExportSource("weight_log", WeightLogEntity.serializer()) {
                listOf(peso("do-backup", 70.0, dia = 19_000))
            },
            ExportSource("water_log", WaterLogEntity.serializer()) {
                listOf(agua("do-backup", dia = 19_000))
            },
            ExportSource("goal_history", GoalHistoryEntity.serializer()) {
                listOf(objetivo("do-backup"))
            },
        ),
        appVersion = "1.0.0",
    ).exportJson()

    private fun importador(terceiraFalha: Boolean) = BackupImporter(
        sources = fontes(terceiraFalha),
        io = Dispatchers.Unconfined,
        db = RoomBackupDb(db),
    )

    @Test
    fun `uma falha a meio do substituir deixa os dados antigos intactos`() = runTest {
        db.weightLogDao().upsert(peso("antigo", 80.0, dia = 20_000))
        db.waterLogDao().upsert(agua("antigo", dia = 20_000))
        val backup = ficheiroDeBackup()

        val r = importador(terceiraFalha = true).import(backup, ImportMode.REPLACE)

        assertIs<ImportResult.Failed>(r, "esperava Failed, veio $r")

        val pesos = db.weightLogDao().exportRows()
        assertEquals(
            listOf("antigo"),
            pesos.map { it.id },
            "o `substituir` apagou as pesagens e a falha não as devolveu — a pessoa " +
                "ficou sem os dados velhos e sem os novos",
        )
        assertEquals(80.0, pesos.single().weightKg)
        assertEquals(
            listOf("antigo"),
            db.waterLogDao().exportRows().map { it.id },
            "a água escrita antes da falha ficou meia escrita",
        )
    }

    @Test
    fun `sem falha, o substituir troca os dados todos`() = runTest {
        db.weightLogDao().upsert(peso("antigo", 80.0, dia = 20_000))
        db.waterLogDao().upsert(agua("antigo", dia = 20_000))
        val backup = ficheiroDeBackup()

        val r = importador(terceiraFalha = false).import(backup, ImportMode.REPLACE)

        assertIs<ImportResult.Done>(r, "esperava Done, veio $r")
        assertEquals(3, r.total)
        assertEquals(listOf("do-backup"), db.weightLogDao().exportRows().map { it.id })
        assertEquals(listOf("do-backup"), db.waterLogDao().exportRows().map { it.id })
        assertEquals(listOf("do-backup"), db.goalHistoryDao().exportRows().map { it.id })
    }

    @Test
    fun `uma falha a meio do juntar nao escreve nada`() = runTest {
        db.weightLogDao().upsert(peso("antigo", 80.0, dia = 20_000))
        val backup = ficheiroDeBackup()

        val r = importador(terceiraFalha = true).import(backup, ImportMode.MERGE)

        assertIs<ImportResult.Failed>(r)
        assertEquals(
            listOf("antigo"),
            db.weightLogDao().exportRows().map { it.id },
            "o `juntar` deixou entrar linhas de uma importação que falhou",
        )
        assertTrue(db.waterLogDao().exportRows().isEmpty())
    }
}

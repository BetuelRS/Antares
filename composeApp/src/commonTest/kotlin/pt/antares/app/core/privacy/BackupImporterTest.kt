package pt.antares.app.core.privacy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import pt.antares.app.core.database.entities.WeightLogEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BackupImporterTest {

    private val gravadas = mutableListOf<WeightLogEntity>()
    private val truncadas = mutableListOf<String>()

    // A transação de mentira corre o bloco na mesma e regista o que lhe mandaram esvaziar.
    // O desfazer a sério só se prova contra uma base verdadeira — ver o
    // `BackupImportRollbackTest`.
    private val transacaoFalsa = BackupDb { aTruncar, bloco ->
        truncadas += aTruncar
        bloco()
    }

    private fun peso(id: String, kg: Double, updatedAt: Long) = WeightLogEntity(
        id = id, epochDay = 20_000, weightKg = kg, note = null,
        updatedAt = updatedAt, deleted = false, dirty = false,
    )

    private fun importador(noTelemovel: List<WeightLogEntity> = emptyList()): BackupImporter {
        gravadas.clear()
        truncadas.clear()
        val fonte = ExportSource(
            "weight_log",
            WeightLogEntity.serializer(),
            restore = { linhas -> gravadas += linhas },
        ) { noTelemovel }
        return BackupImporter(listOf(fonte), Dispatchers.Unconfined, db = transacaoFalsa)
    }

    private suspend fun backupCom(linhas: List<WeightLogEntity>): String =
        DataExporter(
            listOf(ExportSource("weight_log", WeightLogEntity.serializer()) { linhas }),
            appVersion = "0.9.18",
        ).exportJson()

    @Test
    fun `o que sai volta a entrar`() = runTest {

        val backup = backupCom(listOf(peso("a", 80.0, 100), peso("b", 79.0, 200)))

        val r = importador().import(backup, ImportMode.REPLACE)

        assertIs<ImportResult.Done>(r, "esperava Done, veio $r")
        assertEquals(2, r.total)
        assertEquals(listOf("a", "b"), gravadas.map { it.id })
        assertEquals(80.0, gravadas.first { it.id == "a" }.weightKg)
    }

    @Test
    fun `substituir esvazia as tabelas que sabe repor`() = runTest {
        val backup = backupCom(listOf(peso("a", 80.0, 100)))
        importador().import(backup, ImportMode.REPLACE)
        assertEquals(listOf("weight_log"), truncadas, "o `substituir` tem de esvaziar primeiro")
    }

    @Test
    fun `juntar nao esvazia nada`() = runTest {
        val backup = backupCom(listOf(peso("a", 80.0, 100)))
        importador().import(backup, ImportMode.MERGE)
        assertTrue(truncadas.isEmpty(), "o `juntar` apagou dados que devia ter mantido")
    }

    @Test
    fun `ao juntar, a linha mais recente ganha`() = runTest {

        val backup = backupCom(listOf(peso("a", 75.0, updatedAt = 500)))
        val r = importador(noTelemovel = listOf(peso("a", 80.0, updatedAt = 100)))
            .import(backup, ImportMode.MERGE)

        assertIs<ImportResult.Done>(r)
        assertEquals(1, r.total)
        assertEquals(75.0, gravadas.single().weightKg)
    }

    @Test
    fun `ao juntar, o telemovel ganha quando e mais recente`() = runTest {

        val backup = backupCom(listOf(peso("a", 80.0, updatedAt = 100)))
        val r = importador(noTelemovel = listOf(peso("a", 75.0, updatedAt = 500)))
            .import(backup, ImportMode.MERGE)

        assertIs<ImportResult.Done>(r)
        assertEquals(0, r.total, "o backup antigo sobrepôs-se ao que estava no telemóvel")
        assertTrue(gravadas.isEmpty())
    }

    @Test
    fun `empate nao reescreve`() = runTest {

        val backup = backupCom(listOf(peso("a", 80.0, updatedAt = 100)))
        val r = importador(noTelemovel = listOf(peso("a", 80.0, updatedAt = 100)))
            .import(backup, ImportMode.MERGE)

        assertIs<ImportResult.Done>(r)
        assertEquals(0, r.total)
    }

    @Test
    fun `linhas que so existem no backup entram sempre`() = runTest {
        val backup = backupCom(listOf(peso("nova", 70.0, 100)))
        val r = importador(noTelemovel = listOf(peso("velha", 80.0, 900)))
            .import(backup, ImportMode.MERGE)

        assertIs<ImportResult.Done>(r)
        assertEquals(1, r.total)
        assertEquals("nova", gravadas.single().id)
    }

    @Test
    fun `um ficheiro que nao e um backup e recusado, e diz porque`() = runTest {

        assertIs<ImportResult.NotABackup>(importador().import("isto não é JSON", ImportMode.REPLACE))
        assertIs<ImportResult.NotABackup>(importador().import("""{"lista":[]}""", ImportMode.REPLACE))
        assertTrue(truncadas.isEmpty(), "recusou o ficheiro mas já tinha esvaziado a base")
    }

    @Test
    fun `uma tabela sem restore sai do backup mas nao volta`() = runTest {

        val fonte = ExportSource("search_miss", WeightLogEntity.serializer(), restore = null) {
            listOf(peso("x", 1.0, 1))
        }
        val backup = DataExporter(listOf(fonte), appVersion = "0.9.18").exportJson()
        assertTrue(backup.contains("search_miss"), "devia sair no ficheiro")

        truncadas.clear()
        val r = BackupImporter(listOf(fonte), Dispatchers.Unconfined, db = transacaoFalsa)
            .import(backup, ImportMode.REPLACE)

        assertIs<ImportResult.Done>(r)
        assertEquals(0, r.total, "uma tabela sem restore não pode ser escrita")
        assertTrue(
            truncadas.isEmpty(),
            "esvaziou uma tabela que não sabe repor — ficaria vazia para sempre",
        )
    }
}

package pt.antares.app.core.privacy

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupCompleteTest {

    private val vm = File("src/commonMain/kotlin/pt/antares/app/core/privacy/PrivacyViewModel.kt").readText()
    private val partilha = File("src/commonMain/kotlin/pt/antares/app/core/util/FileShare.kt").readText()

    @Test
    fun `o zip transporta bytes, nao so texto`() {
        assertTrue(
            partilha.contains("Map<String, ByteArray>"),
            "o zip voltou a levar só texto — as fotos deixam de caber",
        )
    }

    @Test
    fun `as fotos entram no backup`() {
        assertTrue(vm.contains("photos.readBytes"), "o exportador deixou de ler os ficheiros das fotos")
        assertTrue(vm.contains("BackupFiles.PHOTO_DIR"), "as fotos deixaram de ter sítio no zip")
    }

    @Test
    fun `as fotos voltam do backup, e antes das linhas que as apontam`() {
        assertTrue(vm.contains("photos.writeBytes"), "as fotos não são escritas de volta")

        val escrita = vm.indexOf("photos.writeBytes")
        val linhas = vm.indexOf("importer.import(")
        assertTrue(escrita in 1 until linhas, "as linhas entram antes das fotos")
    }

    @Test
    fun `o nome do ficheiro de dados e o mesmo dos dois lados`() {

        val soltos = Regex(""""antares-dados\.json"""").findAll(vm).count()
        assertEquals(0, soltos, "o nome do ficheiro está escrito à mão em vez de vir do BackupFiles")
        assertTrue(vm.contains("BackupFiles.DATA"))
    }

    @Test
    fun `importar nao fala com a rede`() {
        val importador = File("src/commonMain/kotlin/pt/antares/app/core/privacy/BackupImporter.kt").readText()
        val proibido = Regex("""httpClient|functions\.invoke|postgrest|\.get\("http""")
        assertTrue(!proibido.containsMatchIn(importador), "o importador foi buscar dados à rede")
    }
}

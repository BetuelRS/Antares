package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupRulesTest {

    private val extractionRules =
        File("src/androidMain/res/xml/data_extraction_rules.xml").readText()
    private val fullBackup =
        File("src/androidMain/res/xml/backup_rules.xml").readText()
    private val manifest = File("src/androidMain/AndroidManifest.xml").readText()

    private val todos = listOf(extractionRules, fullBackup)

    @Test
    fun `o manifesto aponta para as duas regras`() {

        assertTrue(
            manifest.contains("android:fullBackupContent=\"@xml/backup_rules\""),
            "falta o fullBackupContent — Android 11 e anteriores copiavam tudo",
        )
        assertTrue(
            manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""),
            "falta o dataExtractionRules — Android 12+ copiava tudo",
        )
    }

    @Test
    fun `os tres ficheiros do SQLite vao juntos`() {

        for (regras in todos) {
            for (ficheiro in listOf("antares.db", "antares.db-wal", "antares.db-shm")) {
                assertTrue(
                    regras.contains("path=\"$ficheiro\""),
                    "falta $ficheiro — o restauro fica inconsistente",
                )
            }
        }
    }

    @Test
    fun `as preferencias vao`() {
        for (regras in todos) {
            assertTrue(regras.contains("antares.preferences_pb"))
        }
    }

    @Test
    fun `nada de fotos na nuvem`() {

        for (regras in todos) {
            for (proibido in listOf("photo", "foto", "imagem", "image")) {
                assertFalse(
                    regras.lowercase().contains("path=\"$proibido"),
                    "as regras incluem algo com \"$proibido\" no caminho",
                )
            }
        }
    }

    @Test
    fun `das SharedPreferences so viaja o idioma`() {

        val permitido = Regex("""<include domain="sharedpref" path="([^"]+)" />""")
        for (regras in todos) {
            val nomeados = permitido.findAll(regras).map { it.groupValues[1] }.distinct().toList()
            assertEquals(listOf("antares_locale.xml"), nomeados)

            assertFalse(regras.contains("""<include domain="sharedpref" path="." />"""))
        }
    }

    @Test
    fun `nada de armazenamento externo nem de cache`() {
        for (regras in todos) {
            assertFalse(regras.contains("domain=\"external\""))
            assertFalse(regras.contains("domain=\"root\""))
        }
    }

    @Test
    fun `so ha inclusoes - a lista e branca`() {

        for (regras in todos) {
            assertFalse(
                regras.contains("<exclude"),
                "apareceu um <exclude>: as regras deixaram de ser lista branca",
            )
        }
    }

    @Test
    fun `a transferencia entre telemoveis segue as mesmas regras`() {

        assertTrue(extractionRules.contains("<device-transfer>"))
        val transferencia = extractionRules.substringAfter("<device-transfer>")
            .substringBefore("</device-transfer>")
        assertTrue(transferencia.contains("antares.db-wal"))

        assertTrue(transferencia.contains("antares_locale.xml"))
        assertFalse(transferencia.contains("path=\".\""))
    }
}

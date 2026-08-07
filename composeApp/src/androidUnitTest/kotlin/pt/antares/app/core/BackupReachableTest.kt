package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class BackupReachableTest {

    private val menu =
        File("src/commonMain/kotlin/pt/antares/app/feature/me/AppMenuScreen.kt").readText()
    private val rotas =
        File("src/commonMain/kotlin/pt/antares/app/navigation/Routes.kt").readText()
    private val navHost =
        File("src/commonMain/kotlin/pt/antares/app/navigation/AntaresNavHost.kt").readText()

    @Test
    fun `o backup esta no menu da engrenagem`() {
        assertTrue(
            menu.contains("backup_title") && menu.contains("onBackupClick"),
            "o backup saiu do menu — volta a estar a três toques, no fim de um ecrã sobre o corpo",
        )
    }

    @Test
    fun `a rota existe e esta ligada`() {
        assertTrue(rotas.contains("data object Backup : Route"), "a rota do backup desapareceu")
        assertTrue(
            navHost.contains("composable<Route.Backup>") && navHost.contains("BackupScreen("),
            "a rota do backup não leva a lado nenhum",
        )
    }

    @Test
    fun `exportar e restaurar continuam nos dois sitios`() {

        val privacidade =
            File("src/commonMain/kotlin/pt/antares/app/feature/profile/ui/PrivacySection.kt").readText()
        assertTrue(
            privacidade.contains("BackupActions("),
            "a secção de privacidade ficou sem exportar — é um direito de GDPR, não uma conveniência",
        )
    }

    @Test
    fun `os botoes sao escritos uma vez so`() {

        val fontes = File("src/commonMain/kotlin/pt/antares/app/feature").walkTopDown()
            .filter { it.extension == "kt" }
            .filter { it.readText().contains("ImportMode.REPLACE") }
            .map { it.name }
            .toList()

        assertTrue(
            fontes.size == 1,
            "o caminho destrutivo do import está escrito em ${fontes.size} sítios: $fontes",
        )
    }
}

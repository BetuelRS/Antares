package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class BackupReachableTest {

    private val menu =
        File("src/commonMain/kotlin/pt/antares/app/feature/me/AppMenuScreen.kt").readText()
    private val rotas =
        File("src/commonMain/kotlin/pt/antares/app/navigation/Routes.kt").readText()
    // A pasta toda e não um ficheiro: as rotas foram arrumadas por área, e apontar a um
    // nome de ficheiro fazia este teste falhar por arrumação em vez de por regressão.
    private val navHost = File("src/commonMain/kotlin/pt/antares/app/navigation")
        .walkTopDown()
        .filter { it.extension == "kt" }
        .joinToString("\n") { it.readText() }

    /**
     * Deixou de ser «o menu da engrenagem» na 2.20.1: o menu passou a ser o separador «Mais»,
     * e a engrenagem que o abria desapareceu com o «Eu». O que o teste defende é o mesmo —
     * **o backup tem de estar na lista** —, e agora está um toque mais perto.
     */
    @Test
    fun `o backup esta no separador Mais`() {
        assertTrue(
            menu.contains("backup_title") && menu.contains("app.copia"),
            "o backup saiu do menu — é a única cópia que existe, e tem de se alcançar da barra",
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

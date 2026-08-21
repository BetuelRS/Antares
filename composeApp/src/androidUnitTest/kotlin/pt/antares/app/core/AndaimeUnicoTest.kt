package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Um andaime só, e é o nosso.
 *
 * Trinta e dois ecrãs usavam o `Scaffold` do Material directamente. Parece inofensivo e não
 * é: o `imePadding` vive no `AntaresScaffold`, e sem ele **o teclado tapa o conteúdo em vez
 * de o empurrar**. Nesses trinta e dois, escrever num campo em baixo do ecrã significava
 * deixar de o ver. O `KeyboardInsetsTest` só apanhava os que tinham campos de texto
 * declarados de uma certa maneira; este apanha a causa, e não os sintomas.
 *
 * O mesmo raciocínio de sempre: uma decisão que se pode desfazer por esquecimento tem de
 * falhar a compilação no dia em que se desfaz.
 */
class AndaimeUnicoTest {

    private val ecras = File("src/commonMain/kotlin/pt/antares/app/feature")
        .walkTopDown()
        .filter { it.extension == "kt" }
        .toList()

    @Test
    fun `nenhum ecra chama o Scaffold do Material directamente`() {
        val infratores = ecras.filter { f ->
            f.readText().lineSequence().any { linha ->
                // A palavra sozinha, e não como sufixo: o `AntaresScaffold` e o
                // `MainScaffold` terminam nela e são exactamente o que se quer.
                Regex("""(^|[^A-Za-z])Scaffold\(""").containsMatchIn(linha) &&
                    !linha.contains("AntaresScaffold(")
            }
        }.map { it.name }

        assertTrue(
            infratores.isEmpty(),
            "estes ecrãs usam o Scaffold cru e perdem o imePadding — o teclado passa a " +
                "tapar o conteúdo: $infratores",
        )
    }

    @Test
    fun `ninguem importa o Scaffold do Material fora do sistema de desenho`() {
        val comImport = ecras
            .filter { it.readText().contains("import androidx.compose.material3.Scaffold") }
            .map { it.name }

        assertTrue(comImport.isEmpty(), "o import voltou em: $comImport")
    }

    @Test
    fun `o andaime partilhado continua a por o imePadding`() {

        // É a razão de existir deste ficheiro. Se alguém tirar esta linha, os trinta e dois
        // ecrãs migrados voltam ao problema todos ao mesmo tempo, e em silêncio.
        val andaime = File(
            "src/commonMain/kotlin/pt/antares/app/core/designsystem/components/Scaffold.kt",
        ).readText()

        assertTrue(
            andaime.contains("imePadding()"),
            "o AntaresScaffold deixou de aplicar o imePadding",
        )
    }
}

package pt.antares.app.core.privacy

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * O par do `CopiaAntigaAindaAbreTest`: aquele prova que um perfil com campos a mais se lê,
 * este prova que é o importador de verdade que o permite. Sem os dois, tirar o
 * `ignoreUnknownKeys` deixava as cópias antigas ilegíveis com a suite toda verde.
 */
class ImportadorToleranteTest {

    @Test
    fun `o importador ignora campos que ja nao conhece`() {
        val fonte = File(
            "src/commonMain/kotlin/pt/antares/app/core/privacy/BackupImporter.kt",
        ).readText()

        assertTrue(
            "ignoreUnknownKeys = true" in fonte,
            "o importador deixou de tolerar campos desconhecidos: toda a cópia feita antes " +
                "de uma coluna sair passa a ser um ficheiro ilegível, e só se descobre no " +
                "dia em que alguém precisar dela",
        )
    }
}

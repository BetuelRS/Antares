package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ThemeAwareColorsTest {

    private val permitidos = setOf(
        "Color.kt",
        "Theme.kt",
        "SupernovaCelebration.kt",
    )

    private fun fontesDeUi(): List<File> =
        File("src/commonMain/kotlin/pt/antares/app")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.name in permitidos }
            .toList()

    @Test
    fun `nenhum ecra escolhe uma variante de tema por sua conta`() {

        val padrao = Regex("""AntaresColors\.\w+(Dark|Light)\b""")
        val infratores = fontesDeUi().mapNotNull { ficheiro ->
            val achados = padrao.findAll(ficheiro.readText()).map { it.value }.toList()
            if (achados.isEmpty()) null else "${ficheiro.name}: ${achados.distinct()}"
        }

        assertTrue(
            infratores.isEmpty(),
            "Variantes de tema usadas fora da camada de tema — em claro isto lê-se " +
                "mal ou lê-se ao contrário. Usa a cor do MaterialTheme (ou " +
                "MaterialTheme.success) e deixa o tema escolher:\n" +
                infratores.joinToString("\n"),
        )
    }

    @Test
    fun `a paleta tem sempre os dois lados de cada par`() {

        val paleta = File("src/commonMain/kotlin/pt/antares/app/core/designsystem/Color.kt").readText()
        val escuras = Regex("""val (\w+)Dark\b""").findAll(paleta).map { it.groupValues[1] }.toSet()
        val claras = Regex("""val (\w+)Light\b""").findAll(paleta).map { it.groupValues[1] }.toSet()

        val orfas = escuras - claras
        assertTrue(orfas.isEmpty(), "cores com variante escura e sem variante clara: $orfas")
    }
}

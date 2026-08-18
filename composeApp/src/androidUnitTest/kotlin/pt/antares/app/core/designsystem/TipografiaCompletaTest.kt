package pt.antares.app.core.designsystem

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Um estilo do Material 3 que a `antaresTypography()` não declare cai no Roboto do
 * sistema — sem erro de compilação, sem aviso, e sem nada no ecrã que o denuncie a não ser
 * a letra estar diferente. Foi assim que 176 usos ficaram fora da fonte da app.
 *
 * A verificação é sobre o texto do ficheiro e não sobre a `Typography` construída: a
 * função é `@Composable` porque carrega as fontes dos recursos, e instanciá-la aqui
 * obrigava a montar o Compose inteiro para contar quinze nomes.
 */
class TipografiaCompletaTest {

    private val estilos = listOf(
        "displayLarge", "displayMedium", "displaySmall",
        "headlineLarge", "headlineMedium", "headlineSmall",
        "titleLarge", "titleMedium", "titleSmall",
        "bodyLarge", "bodyMedium", "bodySmall",
        "labelLarge", "labelMedium", "labelSmall",
    )

    private val ficheiro = File("src/commonMain/kotlin/pt/antares/app/core/designsystem/Type.kt")

    @Test
    fun `os quinze estilos do Material tem fonte da app`() {
        assertTrue(ficheiro.exists(), "não se encontrou o ${ficheiro.name}")
        val texto = ficheiro.readText()

        val faltam = estilos.filterNot { estilo ->
            // A família tem de vir na mesma linha da atribuição: um `displaySmall =
            // TextStyle(fontSize = 30.sp)` compila e continua a cair no Roboto.
            Regex("""$estilo\s*=\s*TextStyle\([^)]*fontFamily\s*=""").containsMatchIn(texto)
        }

        assertTrue(
            faltam.isEmpty(),
            "estes estilos do Material 3 não declaram fonte, e caem no Roboto do sistema:\n" +
                faltam.joinToString("\n") { "  $it" },
        )
    }
}

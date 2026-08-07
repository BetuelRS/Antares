package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NumericFieldsTest {

    private val onboarding =
        File("src/commonMain/kotlin/pt/antares/app/feature/onboarding/OnboardingScreen.kt")

    private fun camposDe(f: File): Int =
        Regex("""OutlinedTextField\(""").findAll(f.readText()).count()

    private fun tecladosDe(f: File): Int =
        Regex("""keyboardOptions\s*=\s*KeyboardOptions\(""").findAll(f.readText()).count()

    @Test
    fun `todos os campos do onboarding pedem teclado numerico`() {
        val campos = camposDe(onboarding)
        val teclados = tecladosDe(onboarding)

        assertTrue(campos > 0, "não encontrei campos no onboarding — o teste deixou de olhar para o sítio certo")
        assertEquals(
            campos,
            teclados,
            "há $campos campos no onboarding e só $teclados declaram teclado. " +
                "Um campo numérico que abre o teclado alfabético obriga a " +
                "procurar o `?123` para escrever o próprio peso",
        )
    }

    @Test
    fun `o peso aceita decimais e a altura nao`() {
        val texto = onboarding.readText()

        assertTrue(
            texto.contains("KeyboardType.Decimal"),
            "o peso e o peso-alvo têm de aceitar vírgula — uma balança dá decimais",
        )
        assertTrue(
            texto.contains("KeyboardType.Number"),
            "a altura e os macros são inteiros",
        )
    }
}

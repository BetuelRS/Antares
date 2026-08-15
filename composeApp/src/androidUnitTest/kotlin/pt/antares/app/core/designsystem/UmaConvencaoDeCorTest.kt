package pt.antares.app.core.designsystem

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A cor diz **uma** coisa de cada vez.
 *
 * Nos macros diz a categoria — proteína, hidratos, gordura —, e nunca o estado. A cor da
 * proteína é avermelhada, e por isso uma barra que mudasse de cor ao passar da meta ficava
 * indistinguível de um aviso: duas convenções na mesma linha, e a pessoa sem saber qual está
 * a ler.
 *
 * Nos micronutrientes, uma barra curta pode ser carência **ou** falta de análise. A diferença
 * está na forma — tracejada quando é incerta — e não na cor, precisamente pela mesma razão.
 */
class UmaConvencaoDeCorTest {

    private val macroBar =
        File("src/commonMain/kotlin/pt/antares/app/core/designsystem/components/MacroBar.kt").readText()

    private val stats =
        File("src/commonMain/kotlin/pt/antares/app/feature/stats/NutritionStatsScreen.kt").readText()

    @Test
    fun `a barra de macro nunca usa uma cor de estado`() {
        val estados = listOf("colorScheme.error", "success", "colorScheme.tertiary")
        val encontradas = estados.filter { it in macroBar }
        assertEquals(
            emptyList(),
            encontradas,
            "a cor de um macro é a categoria dele. Passar da meta mostra-se pela forma — a " +
                "barra a transbordar — e pelo número em negrito",
        )
    }

    @Test
    fun `a barra de macro mostra o excesso`() {
        assertTrue(
            "EXCESS_ALPHA" in macroBar,
            "sem isto a barra trava na meta e passar dela não se vê em lado nenhum a não " +
                "ser no número",
        )
    }

    @Test
    fun `a barra de micro distingue incerteza pela forma`() {
        assertTrue(
            "dashPathEffect" in stats,
            "o tracejado é o que separa «comi pouco» de «não se sabe o que comi»",
        )
        assertTrue(
            "incerta" in stats,
            "a incerteza tem de chegar à barra; escrevê-la só no texto ao lado era o que " +
                "havia antes",
        )
    }

    @Test
    fun `a barra de micro nao troca de cor conforme o valor`() {
        assertTrue(
            "if (c.coveragePct >= 100) MaterialTheme.colorScheme.tertiary" !in stats,
            "verde acima de 100 e azul abaixo é cor de estado, e era ela que fazia uma " +
                "barra curta parecer sempre a mesma coisa",
        )
    }
}

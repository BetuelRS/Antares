package pt.antares.app.core.crash

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CrashReportTest {

    private val stack = listOf(
        "kotlinx.coroutines.internal.ScopeCoroutine.afterResume(Scopes.kt:33)",
        "androidx.compose.runtime.ComposerImpl.doCompose(Composer.kt:3299)",
        "pt.antares.app.feature.workout.ui.WorkoutDetailScreenKt.WorkoutDetail(WorkoutDetailScreen.kt:58)",
        "pt.antares.app.MainActivity.onCreate(MainActivity.kt:33)",
    )

    private fun relatorio(mensagem: String? = "boom", causa: String? = null) =
        CrashReport.format(
            versao = "0.9.18",
            quando = 1_700_000_000_000L,
            thread = "main",
            tipo = "java.lang.IllegalArgumentException",
            mensagem = mensagem,
            stack = stack,
            causa = causa,
        )

    @Test
    fun `traz o que e preciso para identificar a falha`() {
        val r = relatorio()
        assertContains(r, "0.9.18")
        assertContains(r, "1700000000000")
        assertContains(r, "main")
        assertContains(r, "IllegalArgumentException")
        assertContains(r, "boom")
        assertContains(r, "WorkoutDetailScreen.kt:58")
    }

    @Test
    fun `sem mensagem diz que nao havia, em vez de deixar vazio`() {

        assertContains(relatorio(mensagem = null), "(nenhuma)")
    }

    @Test
    fun `a causa aparece quando existe`() {
        val r = relatorio(causa = "NullPointerException: name")
        assertContains(r, "causa: NullPointerException: name")

        assertTrue(!relatorio().contains("causa:"))
    }

    @Test
    fun `stacks enormes sao cortados e dizem quanto cortaram`() {

        val enorme = (1..100).map { "pt.antares.app.Frame$it(F.kt:$it)" }
        val r = CrashReport.format("0.9.18", 1L, "main", "E", null, enorme)

        assertContains(r, "Frame1(")
        assertContains(r, "mais ${100 - CrashReport.MAX_FRAMES} linhas")
        assertTrue(!r.contains("Frame100("), "não devia ter chegado ao fim do stack")
    }

    @Test
    fun `o culpado e a primeira linha que e codigo desta app`() {

        assertEquals(
            "pt.antares.app.feature.workout.ui.WorkoutDetailScreenKt.WorkoutDetail(WorkoutDetailScreen.kt:58)",
            CrashReport.culpado(stack),
        )
    }

    @Test
    fun `sem codigo nosso no stack nao inventa um culpado`() {
        val soBibliotecas = listOf("android.os.Handler.dispatchMessage(Handler.java:106)")
        assertNull(CrashReport.culpado(soBibliotecas))
    }
}

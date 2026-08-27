package pt.antares.app.feature.ai

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Duas ligações da folha da AI que não se conseguem provar com um teste de estado, porque
 * não são estado: são onde uma coisa está escrita no ecrã.
 *
 * Lêem-se os ficheiros, como o `CartaoUnicoTest` e o `AccessibilityTest` já fazem. Um teste
 * de fonte é grosseiro, e é a única coisa que apanha uma regressão que não muda nenhum
 * número — que é exactamente o feitio das duas que estão aqui.
 */
class FolhaDaIaTest {

    private fun ler(caminho: String): List<String> =
        File(caminho).readText().replace("\r\n", "\n").split("\n")

    private val folha = "src/commonMain/kotlin/pt/antares/app/feature/ai/AiSheet.kt"
    private val barra = "src/commonMain/kotlin/pt/antares/app/feature/fooddata/QuickLogBar.kt"

    /**
     * O aviso legal vive dentro da revisão, e em mais lado nenhum da folha.
     *
     * Até à 2.17.0 estava fora do `when` e aparecia nas quatro fases — sobre um campo de
     * texto vazio, sobre um indicador a rodar, e sobre uma mensagem de erro. Um aviso que
     * aparece onde não há números nenhuns deixa de se ler onde há.
     */
    @Test
    fun `o aviso legal esta dentro da revisao`() {
        val linhas = ler(folha)
        val usos = linhas.withIndex()
            .filter { (_, l) -> "Res.string.ai_disclaimer" in l && !l.trimStart().startsWith("import") }
            .map { it.index }

        assertEquals(1, usos.size, "o aviso legal aparece ${usos.size} vezes na folha")

        val revisao = linhas.indexOfFirst { "private fun ReviewStep(" in it }
        assertTrue(revisao > 0, "não achei a revisão")

        val seguinte = linhas.withIndex()
            .first { (i, l) -> i > revisao && l.startsWith("@Composable") }
            .index

        assertTrue(
            usos.single() in (revisao + 1) until seguinte,
            "o aviso legal saiu da revisão — está na linha ${usos.single() + 1}",
        )
    }

    /**
     * A voz não vai para a pesquisa.
     *
     * O texto que se mostra a quem carrega no microfone é «diz o que comeste» — pede uma
     * frase de refeição, com quantidades e mais do que um alimento. Entregue a uma pesquisa
     * de catálogo, «dois ovos e uma torrada» não encontra nada: não é nome de alimento
     * nenhum. Vai para a folha da AI, que é a única coisa nesta app que lê uma frase dessas.
     */
    @Test
    fun `o microfone entrega a voz e nao a pesquisa`() {
        val texto = File(barra).readText().replace("\r\n", "\n")
        // `substringAfterLast`, e não `substringAfter`: a primeira ocorrência do nome é a
        // linha do import, e a partir dela o bloco apanhava o `submit()` — que chama mesmo
        // a `onSubmit`, e dava o teste vermelho pela razão errada.
        val bloco = texto.substringAfterLast("rememberVoiceInput").substringBefore("}")

        assertTrue("onVoice(heard)" in bloco, "o microfone deixou de entregar a onVoice")
        assertTrue("onSubmit" !in bloco, "o microfone voltou a cair na pesquisa")
    }

    /** E os dois ecrãs que a mostram levam o ditado à AI, e não à procura. */
    @Test
    fun `os dois ecras mandam a voz para a folha da IA`() {
        for (ecra in listOf(
            "src/commonMain/kotlin/pt/antares/app/feature/diary/DiaryScreen.kt",
            "src/commonMain/kotlin/pt/antares/app/feature/today/TodayScreen.kt",
        )) {
            val texto = File(ecra).readText().replace("\r\n", "\n")
            val bloco = texto.substringAfter("onVoice = ").substringBefore("\n            }")

            assertTrue(
                "AddMode.DESCRIBE" in bloco,
                "$ecra manda o ditado para outro sítio que não a folha da AI",
            )
        }
    }
}

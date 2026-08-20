package pt.antares.app.core.privacy

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A rotação apaga ficheiros e não os pode apagar pelos errados: a cópia que sobra é a que
 * alguém vai usar no pior dia que a app lhe der. Toda a garantia assenta numa coisa só — o
 * nome ordenar-se sozinho pela data — e é isso que estes testes fixam.
 */
class CopiaAutomaticaTest {

    private fun em(iso: String) = Instant.parse(iso)

    // Fuso fixo: sem ele o teste passava em Londres e falhava em Lisboa.
    private fun nome(quando: Instant) = AutoBackup.nomeDe(quando, TimeZone.UTC)

    @Test
    fun `o nome leva a data e a hora`() {
        assertEquals(
            "antares-copia-2026-08-20-143207.zip",
            nome(em("2026-08-20T14:32:07Z")),
        )
    }

    @Test
    fun `os numeros de um algarismo levam zero a frente`() {

        // Sem o zero, «9» viria depois de «10» em ordem alfabética, e a rotação apagava a
        // cópia mais recente do dia em vez da mais antiga.
        assertEquals("antares-copia-2026-01-02-090500.zip", nome(em("2026-01-02T09:05:00Z")))
    }

    @Test
    fun `a ordem alfabetica dos nomes e a ordem do tempo`() {
        val momentos = listOf(
            "2025-12-31T23:59:00Z",
            "2026-01-01T00:01:00Z",
            "2026-01-02T09:00:00Z",
            "2026-01-02T10:00:00Z",
            "2026-09-30T18:00:00Z",
            "2026-10-01T08:00:00Z",
        ).map { em(it) }

        val nomes = momentos.map { nome(it) }
        assertEquals(nomes, nomes.sorted(), "a ordem alfabética deixou de seguir o tempo")
    }

    @Test
    fun `a rotacao apaga as mais antigas e nunca as recentes`() {
        val nomes = listOf(
            "antares-copia-2026-08-01-080000.zip",
            "antares-copia-2026-08-04-080000.zip",
            "antares-copia-2026-08-07-080000.zip",
            "antares-copia-2026-08-10-080000.zip",
            "antares-copia-2026-08-13-080000.zip",
            "antares-copia-2026-08-16-080000.zip",
            "antares-copia-2026-08-19-080000.zip",
        )

        val apagar = AutoBackup.aApagar(nomes, maximo = 5)
        assertEquals(
            listOf(
                "antares-copia-2026-08-01-080000.zip",
                "antares-copia-2026-08-04-080000.zip",
            ),
            apagar,
        )
    }

    @Test
    fun `a rotacao nao se importa com a ordem em que a pasta foi lida`() {

        // O sistema de ficheiros não promete ordem nenhuma, e no MediaStore a listagem sai
        // pela ordem de inserção — que deixa de ser cronológica assim que uma cópia é
        // apagada e outra escrita por cima do lugar dela.
        val baralhados = listOf(
            "antares-copia-2026-08-19-080000.zip",
            "antares-copia-2026-08-01-080000.zip",
            "antares-copia-2026-08-13-080000.zip",
            "antares-copia-2026-08-04-080000.zip",
            "antares-copia-2026-08-16-080000.zip",
            "antares-copia-2026-08-07-080000.zip",
        )

        assertEquals(
            listOf("antares-copia-2026-08-01-080000.zip"),
            AutoBackup.aApagar(baralhados, maximo = 5),
        )
    }

    @Test
    fun `com cinco ou menos nao se apaga nada`() {
        val cinco = (1..5).map { "antares-copia-2026-08-0$it-080000.zip" }
        assertTrue(AutoBackup.aApagar(cinco, maximo = 5).isEmpty())
        assertTrue(AutoBackup.aApagar(emptyList(), maximo = 5).isEmpty())
    }

    @Test
    fun `o aviso chega depois da cadencia`() {

        // Se avisasse antes de a cópia seguinte ser devida, o cartão ficava vermelho num dia
        // em que a app estava a fazer exatamente o que prometeu.
        assertTrue(
            AutoBackup.DIAS_ATE_AVISAR > AutoBackup.DIAS_ENTRE_COPIAS,
            "o aviso passou a chegar antes de a cópia seguinte ser devida",
        )
    }
}

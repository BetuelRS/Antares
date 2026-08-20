package pt.antares.app.core.privacy

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Duas cópias seguidas não podem pedir o mesmo nome.
 *
 * Só se viu ao correr a app: a primeira cópia automática ficou gravada como
 * `antares-copia-2026-08-20-1931.zip`, e uma segunda no mesmo minuto pedia esse nome outra
 * vez. O MediaStore resolve-o sozinho — grava `antares-copia-2026-08-20-1931 (1).zip` —, e
 * esse nome tem duas consequências: deixa de se ordenar pela data, o que faz a rotação
 * escolher mal, e não é o nome que a app julga ter escrito.
 */
class NomesDeCopiaUnicosTest {

    private fun nome(iso: String) = AutoBackup.nomeDe(Instant.parse(iso), TimeZone.UTC)

    @Test
    fun `dois momentos do mesmo minuto dao nomes diferentes`() {
        assertTrue(
            nome("2026-08-20T19:31:04Z") != nome("2026-08-20T19:31:41Z"),
            "duas cópias no mesmo minuto pedem o mesmo nome ao sistema de ficheiros",
        )
    }

    @Test
    fun `os segundos nao quebram a ordem`() {
        val seguidos = listOf(
            "2026-08-20T19:31:04Z",
            "2026-08-20T19:31:41Z",
            "2026-08-20T19:32:00Z",
            "2026-08-20T20:00:00Z",
        ).map { nome(it) }

        assertEquals(seguidos, seguidos.sorted())
    }

    @Test
    fun `o nome nunca leva parenteses`() {

        // O «(1)» é a marca de um nome atribuído pelo sistema e não pela app. Se aparecer
        // num nome gerado aqui, alguma coisa passou a construí-lo noutro sítio.
        assertTrue('(' !in nome("2026-08-20T19:31:04Z"))
    }
}

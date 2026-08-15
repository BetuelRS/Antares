package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A app tinha jejum e tinha diário, e os dois não se conheciam: dava para ter o contador a
 * subir no ecrã e três refeições registadas por baixo dele. Isto é o que os liga.
 */
class FastingClashTest {

    private val inicio = 1_700_000_000_000L
    private val hora = 3_600_000L

    @Test
    fun `so contam os registos entre o inicio do jejum e agora`() {
        val instantes = listOf(
            inicio - hora,
            inicio + hora,
            inicio + 2 * hora,
            inicio + 10 * hora,
        )

        val dentro = FastingClash.dentroDoJejum(instantes, inicioMs = inicio, agoraMs = inicio + 3 * hora)

        assertEquals(listOf(inicio + hora, inicio + 2 * hora), dentro)
    }

    @Test
    fun `comer antes de comecar o jejum nao e quebra nenhuma`() {
        val antes = listOf(inicio - hora, inicio - 5 * hora)
        assertTrue(FastingClash.dentroDoJejum(antes, inicio, inicio + hora).isEmpty())
    }

    @Test
    fun `o instante em que o jejum comeca conta como dentro`() {
        assertEquals(
            listOf(inicio),
            FastingClash.dentroDoJejum(listOf(inicio), inicio, inicio + hora),
            "quem regista a última refeição à hora exata do arranque está a marcar o fim " +
                "dela, não uma quebra — mas contá-la é o lado seguro: a app diz o facto e " +
                "quem decide é a pessoa",
        )
    }

    @Test
    fun `sem registos nao ha nada a dizer`() {
        assertTrue(FastingClash.dentroDoJejum(emptyList(), inicio, inicio + hora).isEmpty())
    }

    @Test
    fun `a lista sai por ordem, para a primeira quebra ser a primeira`() {
        val baralhados = listOf(inicio + 5 * hora, inicio + hora, inicio + 3 * hora)
        assertEquals(
            listOf(inicio + hora, inicio + 3 * hora, inicio + 5 * hora),
            FastingClash.dentroDoJejum(baralhados, inicio, inicio + 6 * hora),
        )
    }
}

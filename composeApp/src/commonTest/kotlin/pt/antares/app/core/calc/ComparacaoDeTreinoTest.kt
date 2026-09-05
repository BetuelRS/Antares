package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ComparacaoDeTreinoTest {

    private fun treino(min: Int, volume: Double, series: Int) =
        TreinoComparavel(duracaoMin = min, volume = volume, series = series)

    private val hoje = treino(min = 52, volume = 9338.0, series = 18)

    @Test
    fun `a ultima vez e a mais recente das anteriores`() {
        val c = ComparacaoDoTreino.de(
            hoje,
            listOf(
                treino(min = 56, volume = 8902.0, series = 18),
                treino(min = 60, volume = 8000.0, series = 16),
            ),
        )
        val ultima = requireNotNull(c.ultimaVez)
        assertEquals(-4, ultima.duracaoMin)
        assertEquals(436.0, ultima.volume)
        assertEquals(0, ultima.series)
    }

    /**
     * A diferença viaja com o termo de que partiu. Sem ele, «+436 kg» não diz de onde veio, e
     * o ecrã teria de ir buscar o número a outro sítio para escrever a mesma frase.
     */
    @Test
    fun `a diferenca leva o treino com que se comparou`() {
        val anterior = treino(min = 56, volume = 8902.0, series = 18)
        val c = ComparacaoDoTreino.de(hoje, listOf(anterior))
        assertEquals(anterior, requireNotNull(c.ultimaVez).referencia)
    }

    /**
     * A média é das três, e só existe com três. Com duas, «média das últimas três» era mentir
     * sobre o que se somou.
     */
    @Test
    fun `a media so aparece com tres treinos anteriores`() {
        val dois = listOf(
            treino(min = 50, volume = 9000.0, series = 17),
            treino(min = 50, volume = 9000.0, series = 17),
        )
        assertNull(ComparacaoDoTreino.de(hoje, dois).media)

        val tres = dois + treino(min = 50, volume = 9000.0, series = 17)
        assertEquals(3, ComparacaoDoTreino.TREINOS_DA_MEDIA)
        assertEquals(2, requireNotNull(ComparacaoDoTreino.de(hoje, tres).media).duracaoMin)
    }

    /** Uma quarta sessão mais antiga não entra na média das três. */
    @Test
    fun `a media usa as tres mais recentes e nao as que houver`() {
        val c = ComparacaoDoTreino.de(
            hoje,
            listOf(
                treino(min = 50, volume = 9000.0, series = 18),
                treino(min = 50, volume = 9000.0, series = 18),
                treino(min = 50, volume = 9000.0, series = 18),
                // Um treino muito antigo e muito curto: se entrasse, a média descia.
                treino(min = 10, volume = 1000.0, series = 3),
            ),
        )
        val media = requireNotNull(c.media)
        assertEquals(9000.0, media.referencia.volume)
        assertEquals(50, media.referencia.duracaoMin)
    }

    /**
     * Os minutos e as séries são contagens inteiras, e a média arredonda-se **antes** de a
     * diferença ser feita: senão o número mostrado não fechava com os números mostrados.
     */
    @Test
    fun `a media arredonda os minutos e as series antes de subtrair`() {
        val c = ComparacaoDoTreino.de(
            treino(min = 52, volume = 9000.0, series = 18),
            listOf(
                treino(min = 50, volume = 9000.0, series = 17),
                treino(min = 51, volume = 9000.0, series = 17),
                treino(min = 51, volume = 9000.0, series = 18),
            ),
        )
        val media = requireNotNull(c.media)
        // (50 + 51 + 51) / 3 = 50,67 → 51 minutos, e a diferença é de um.
        assertEquals(51, media.referencia.duracaoMin)
        assertEquals(1, media.duracaoMin)
        // (17 + 17 + 18) / 3 = 17,33 → 17 séries.
        assertEquals(17, media.referencia.series)
        assertEquals(1, media.series)
    }

    /**
     * A primeira vez que uma rotina é feita não tem com que se comparar, e a comparação
     * devolve as duas vazias em vez de zeros — zero é uma diferença, e não houve nenhuma.
     */
    @Test
    fun `a primeira vez de uma rotina nao compara com nada`() {
        val c = ComparacaoDoTreino.de(hoje, emptyList())
        assertNull(c.ultimaVez)
        assertNull(c.media)
    }
}

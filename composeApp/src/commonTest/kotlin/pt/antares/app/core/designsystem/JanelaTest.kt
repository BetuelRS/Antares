package pt.antares.app.core.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * As fronteiras da janela são as do Material 3, e este teste guarda-as pelos números e não
 * pelas constantes: escrever `LARGURA_MEDIA_DP` dos dois lados faria o teste concordar com
 * qualquer valor que alguém lá pusesse, incluindo um errado.
 *
 * Verificam-se os dois lados de cada fronteira. Um `>` trocado por um `>=` num telemóvel de
 * 600 dp deitado é exatamente o tipo de engano que só aparece num dispositivo.
 */
class JanelaTest {

    @Test
    fun `as fronteiras da largura sao 600 e 840`() {
        assertEquals(LarguraDaJanela.COMPACTA, larguraDaJanela(360))
        assertEquals(LarguraDaJanela.COMPACTA, larguraDaJanela(599))
        assertEquals(LarguraDaJanela.MEDIA, larguraDaJanela(600))
        assertEquals(LarguraDaJanela.MEDIA, larguraDaJanela(839))
        assertEquals(LarguraDaJanela.LARGA, larguraDaJanela(840))
        assertEquals(LarguraDaJanela.LARGA, larguraDaJanela(1280))
    }

    @Test
    fun `a fronteira da altura e 480`() {
        assertEquals(AlturaDaJanela.BAIXA, alturaDaJanela(360))
        assertEquals(AlturaDaJanela.BAIXA, alturaDaJanela(479))
        assertEquals(AlturaDaJanela.NORMAL, alturaDaJanela(480))
        assertEquals(AlturaDaJanela.NORMAL, alturaDaJanela(800))
    }

    @Test
    fun `cada largura tem um modo, e sao tres`() {
        assertEquals(ModoDeEsquema.UMA_COLUNA, modoDeEsquema(LarguraDaJanela.COMPACTA))
        assertEquals(ModoDeEsquema.LISTA_E_DETALHE, modoDeEsquema(LarguraDaJanela.MEDIA))
        assertEquals(ModoDeEsquema.DUAS_COLUNAS, modoDeEsquema(LarguraDaJanela.LARGA))
        assertEquals(3, ModoDeEsquema.entries.size)
    }

    @Test
    fun `o telemovel de pe e o unico que fica com a barra em baixo`() {
        assertFalse(navegacaoAoLado(LarguraDaJanela.COMPACTA, AlturaDaJanela.NORMAL))

        // Deitado: a largura ainda pode ser compacta (um telemóvel pequeno dá 560 dp), e o
        // que manda a navegação para o lado é a altura que sobra para o conteúdo.
        assertTrue(navegacaoAoLado(LarguraDaJanela.COMPACTA, AlturaDaJanela.BAIXA))
        assertTrue(navegacaoAoLado(LarguraDaJanela.MEDIA, AlturaDaJanela.NORMAL))
        assertTrue(navegacaoAoLado(LarguraDaJanela.LARGA, AlturaDaJanela.NORMAL))
    }
}

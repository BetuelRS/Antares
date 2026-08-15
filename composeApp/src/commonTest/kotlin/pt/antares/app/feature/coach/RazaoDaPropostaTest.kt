package pt.antares.app.feature.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * O cartão que propõe mudar a meta tem de dizer porquê antes de pedir a decisão. A frase é
 * a comparação entre o ritmo pedido e o ritmo real — e o que aqui se fixa é **quando é que
 * ela se cala**, que é a parte fácil de estragar.
 */
class RazaoDaPropostaTest {

    @Test
    fun `pediu perder e esta a perder devagar`() {
        assertEquals(RazaoDaProposta.A_PERDER, razaoDaProposta(pedido = -0.5, real = -0.28))
    }

    @Test
    fun `pediu perder e esta a perder depressa de mais`() {
        assertEquals(RazaoDaProposta.A_PERDER, razaoDaProposta(pedido = -0.5, real = -0.9))
    }

    @Test
    fun `pediu ganhar e esta a ganhar`() {
        assertEquals(RazaoDaProposta.A_GANHAR, razaoDaProposta(pedido = 0.25, real = 0.10))
    }

    @Test
    fun `pediu perder e esta a ganhar`() {
        assertEquals(
            RazaoDaProposta.AO_CONTRARIO,
            razaoDaProposta(pedido = -0.5, real = 0.2),
            "o peso a ir ao contrário é o caso que mais precisa de ser dito",
        )
    }

    @Test
    fun `pediu ganhar e esta a perder`() {
        assertEquals(RazaoDaProposta.AO_CONTRARIO, razaoDaProposta(pedido = 0.3, real = -0.1))
    }

    @Test
    fun `sem ritmo real nao ha frase`() {
        assertNull(
            razaoDaProposta(pedido = -0.5, real = null),
            "uma semana com menos de duas pesagens não sabe para onde o peso foi",
        )
    }

    @Test
    fun `sem perfil nao ha frase`() {
        assertNull(razaoDaProposta(pedido = null, real = -0.3))
    }

    @Test
    fun `quem pede manter o peso nao tem ritmo para comparar`() {
        assertNull(razaoDaProposta(pedido = 0.0, real = -0.3))
        assertNull(
            razaoDaProposta(pedido = -0.04, real = -0.3),
            "abaixo de 50 g por semana o pedido é manter",
        )
    }

    @Test
    fun `mesmo com o pedido a zero, 50 g ja conta`() {
        assertEquals(RazaoDaProposta.A_PERDER, razaoDaProposta(pedido = -RITMO_MINIMO_KG, real = -0.3))
    }

    @Test
    fun `peso parado conta como estar do lado certo, e nao ao contrario`() {
        assertEquals(
            RazaoDaProposta.A_PERDER,
            razaoDaProposta(pedido = -0.5, real = 0.0),
            "zero não tem sinal: dizer que o peso vai ao contrário seria afirmar de mais",
        )
    }
}

package pt.antares.app.core.designsystem.components

import pt.antares.app.core.designsystem.LarguraDaJanela
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * O teto de três colunas é uma decisão, não um acaso: acima disso cada coluna fica estreita
 * de mais para o nome de um alimento, que é o texto mais comprido destas listas. A janela
 * média fica-se por duas — 600 dp a dividir por três dava 200 dp por coluna.
 */
class ListaAdaptavelTest {

    @Test
    fun `as colunas da lista sao uma, duas e tres`() {
        assertEquals(1, colunasDaLista(LarguraDaJanela.COMPACTA))
        assertEquals(2, colunasDaLista(LarguraDaJanela.MEDIA))
        assertEquals(3, colunasDaLista(LarguraDaJanela.LARGA))
    }
}

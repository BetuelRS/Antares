package pt.antares.app.feature.fooddata

import pt.antares.app.core.database.daos.MarcaDeUtilizadorRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * O que é da pessoa sobrevive à troca do catálogo.
 *
 * Desde a 2.4.0 o catálogo é substituído por inteiro a cada versão, e a escrita grava a linha
 * toda por cima. Quatro colunas não vêm do ficheiro — o favorito, a última utilização, a
 * porção guardada e a lápide — e **perdê-las não dá erro nenhum**: a pessoa é que, um dia,
 * abre os favoritos e estão vazios.
 *
 * É por isso que a montagem da linha é uma função à parte e recebe a marca em vez de a ir
 * buscar: assim tem um teste que não precisa de base de dados nenhuma, e que falha no
 * instante em que alguém tirar uma destas quatro linhas do sítio.
 */
class MarcaDoUtilizadorSobreviveTest {

    private val doFicheiro = AlimentoDoCatalogo(
        id = "ciqual-1000",
        source = "SEED",
        nameEn = "Pastis",
        namePt = "Pastis",
        kcal = 300,
        proteinG = 0.1,
        carbsG = 2.0,
        fatG = 0.0,
        isLiquid = true,
        verified = true,
    )

    private val marca = MarcaDeUtilizadorRow(
        id = "ciqual-1000",
        isFavorite = true,
        lastUsedAt = 1_700_000_000_000L,
        lastAmountG = 45.0,
        deleted = false,
    )

    @Test
    fun `o favorito, o recente e a porcao viajam para a linha nova`() {
        val linha = linhaDe(doFicheiro, marca, agora = 99L)

        assertTrue(linha.isFavorite, "o favorito perdeu-se na troca do catálogo")
        assertEquals(1_700_000_000_000L, linha.lastUsedAt, "a última utilização perdeu-se")
        assertEquals(45.0, linha.lastAmountG, "a porção guardada perdeu-se")
    }

    @Test
    fun `a lapide da pessoa nao e levantada pelo ficheiro`() {

        // Esconder um alimento é uma decisão. Reescrevê-lo do catálogo fá-lo reaparecer na
        // pesquisa no dia seguinte, e a pessoa esconde-o outra vez sem perceber porquê.
        val linha = linhaDe(doFicheiro, marca.copy(deleted = true), agora = 99L)
        assertTrue(linha.deleted, "a lápide foi levantada pela importação")
    }

    @Test
    fun `um alimento que a pessoa nunca tocou entra limpo`() {
        val linha = linhaDe(doFicheiro, marca = null, agora = 99L)

        assertFalse(linha.isFavorite)
        assertEquals(0L, linha.lastUsedAt)
        assertEquals(null, linha.lastAmountG)
        assertFalse(linha.deleted)
    }

    @Test
    fun `o resto da linha vem todo do ficheiro`() {

        // A marca não pode ganhar terreno: se um dia levar também o nome ou a nutrição, uma
        // correção ao catálogo deixa de chegar exactamente a quem já usou aquele alimento —
        // que são as pessoas a quem ela mais interessa.
        val linha = linhaDe(doFicheiro.copy(namePt = "Pastis corrigido", kcal = 280), marca, agora = 99L)

        assertEquals("Pastis corrigido", linha.namePt)
        assertEquals(280, linha.kcal)
        assertTrue(linha.isLiquid)
        assertTrue(linha.verified)
        assertEquals(99L, linha.updatedAt, "o instante da instalação é o que a poda usa para decidir")
    }
}

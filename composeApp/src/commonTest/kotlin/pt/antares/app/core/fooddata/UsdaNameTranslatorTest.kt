package pt.antares.app.core.fooddata

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A regra é «tudo ou nada», e é ela que este teste defende.
 *
 * A app já teve nomes traduzidos palavra a palavra — «Pie, Dutch Maçã, Comercial» — e o
 * remédio da altura foi apagar as traduções todas e mostrar inglês. Um nome meio traduzido
 * é pior do que qualquer das duas línguas: não se lê, não se procura, e faz a app parecer
 * partida.
 */
class UsdaNameTranslatorTest {

    private val segmentos = mapOf(
        "chicken" to "frango",
        "breast" to "peito",
        "raw" to "cru",
        "rice" to "arroz",
        "white" to "branco",
        "cooked" to "cozinhado",
        "bread" to "pão",
        "sweet potato" to "batata-doce",
        "low sodium" to "baixo teor de sódio",
    )

    @Test
    fun `um nome inteiramente coberto fica em portugues`() {
        assertEquals(
            "Frango, peito, cru",
            UsdaNameTranslator.traduzir("Chicken, breast, raw", segmentos),
        )
    }

    @Test
    fun `um segmento por traduzir chega para desistir do nome todo`() {
        assertNull(
            UsdaNameTranslator.traduzir("Chicken, breast, smoked", segmentos),
            "«smoked» não está no dicionário: devolver «Frango, peito, smoked» era exatamente " +
                "o defeito que se está a corrigir",
        )
    }

    @Test
    fun `um descritor de varias palavras traduz-se como um so`() {
        assertEquals(
            "Batata-doce, cozinhado",
            UsdaNameTranslator.traduzir("Sweet potato, cooked", segmentos),
            "traduzir palavra a palavra dava «doce batata»",
        )
        assertEquals(
            "Arroz, baixo teor de sódio",
            UsdaNameTranslator.traduzir("Rice, low sodium", segmentos),
            "«low sodium» não é «baixo sódio»: traduz-se o descritor, não as palavras",
        )
    }

    @Test
    fun `a forma com virgulas le-se igual nas duas linguas`() {
        // «Rice, white» dá «Arroz, branco», que é a ordem certa em português. É por isso
        // que se traduz a forma com vírgulas e não a forma já arrumada em inglês.
        assertEquals("Arroz, branco", UsdaNameTranslator.traduzir("Rice, white", segmentos))
    }

    @Test
    fun `nomes de marca nao se traduzem`() {
        assertNull(
            UsdaNameTranslator.traduzir("Bread, KELLOGGS special", segmentos),
            "uma marca em maiúsculas denuncia um produto de embalagem, e traduzir marcas dá " +
                "nomes que ninguém encontra",
        )
    }

    @Test
    fun `segmentos sem letras passam como estao`() {
        assertEquals(
            "Arroz, branco, 100 g",
            UsdaNameTranslator.traduzir("Rice, white, 100 g", segmentos),
            "«100 g» não é uma palavra por traduzir, e exigi-lo no dicionário deitava fora " +
                "o nome inteiro",
        )
    }

    @Test
    fun `um nome vazio nao vira uma traducao vazia`() {
        assertNull(UsdaNameTranslator.traduzir("", segmentos))
        assertNull(UsdaNameTranslator.traduzir("   ", segmentos))
        assertNull(UsdaNameTranslator.traduzir(",,,", segmentos))
    }

    @Test
    fun `a primeira letra fica maiuscula`() {
        assertEquals("Pão, cru", UsdaNameTranslator.traduzir("bread, raw", segmentos))
    }

    @Test
    fun `a procura nao distingue maiusculas`() {
        assertEquals("Arroz, cru", UsdaNameTranslator.traduzir("Rice, Raw", segmentos))
    }
}

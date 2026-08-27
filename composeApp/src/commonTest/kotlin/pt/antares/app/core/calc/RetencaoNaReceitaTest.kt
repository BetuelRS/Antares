package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A receita cozinhada perde o que o lume destrói.
 *
 * Até à 2.7.0 não perdia: os ingredientes somavam-se crus e a água evaporada **concentrava**
 * tudo, vitamina C incluída. Uma sopa de espinafres saía do tacho com mais vitamina C do que
 * os espinafres que lá entraram, e nada no ecrã dizia que isso é impossível.
 *
 * Estes testes cobram as três coisas que a correcção tem de garantir: que a retenção se
 * aplica, que se aplica **por ingrediente**, e que uma receita sem método fica exactamente
 * como estava.
 */
class RetencaoNaReceitaTest {

    private val vitC = "vitC_mg"
    private val ferro = "iron_mg"

    /** Espinafres: 28 mg de vitamina C por 100 g, e cozer guarda 75 %. */
    private fun espinafres(gramas: Double, retencoes: Map<String, Double> = emptyMap()) =
        IngredientNutrition(
            kcalPer100 = 23,
            proteinPer100 = 2.9,
            carbsPer100 = 3.6,
            fatPer100 = 0.4,
            grams = gramas,
            microsPer100 = mapOf(vitC to 28.0, ferro to 2.7),
            retencoes = retencoes,
        )

    @Test
    fun `sem metodo a receita fica como estava`() {
        val cru = RecipeCalc.compute(listOf(espinafres(500.0)), yieldGrams = null)

        // 500 g a 28 mg/100 g = 140 mg em 500 g, que é 28 mg por 100 g.
        assertEquals(28.0, cru.microsPer100[vitC]!!, 1e-9)
    }

    @Test
    fun `cozer destroi um quarto da vitamina C`() {
        val cozido = RecipeCalc.compute(
            listOf(espinafres(500.0, mapOf(vitC to 0.75))),
            yieldGrams = 500.0,
        )

        assertEquals(21.0, cozido.microsPer100[vitC]!!, 1e-9)
    }

    /**
     * O caso que motivou tudo: perder água **e** perder vitamina.
     *
     * 500 g de espinafres com 140 mg de vitamina C reduzem-se a 400 g. Sem retenção, os
     * 140 mg concentravam-se em 35 mg/100 g — mais do que os 28 de partida. Com retenção,
     * são 105 mg em 400 g, ou seja 26,25 — menos, que é o que acontece na realidade.
     */
    @Test
    fun `a agua concentra mas a retencao chega primeiro`() {
        val ingredientes = listOf(espinafres(500.0, mapOf(vitC to 0.75)))

        val semRetencao = RecipeCalc.compute(listOf(espinafres(500.0)), yieldGrams = 400.0)
        val comRetencao = RecipeCalc.compute(ingredientes, yieldGrams = 400.0)

        assertEquals(35.0, semRetencao.microsPer100[vitC]!!, 1e-9)
        assertEquals(26.25, comRetencao.microsPer100[vitC]!!, 1e-9)

        assertTrue(
            comRetencao.microsPer100[vitC]!! < 28.0,
            "cozinhar não pode deixar 100 g com mais vitamina C do que tinham em cru",
        )
    }

    @Test
    fun `os minerais quase nao se perdem`() {
        val cozido = RecipeCalc.compute(
            listOf(espinafres(500.0, mapOf(vitC to 0.75, ferro to 0.95))),
            yieldGrams = 500.0,
        )

        assertEquals(2.565, cozido.microsPer100[ferro]!!, 1e-9)
    }

    /**
     * A retenção é de cada ingrediente, e não da receita.
     *
     * Cozer destrói vitamina C nos legumes e não tem nada a destruir no azeite. Um factor
     * único aplicado ao prato inteiro descontava vitamina a quem não a tinha — e, pior,
     * descontava-a na proporção errada assim que as quantidades mudassem.
     */
    @Test
    fun `cada ingrediente perde o que a familia dele perde`() {
        val azeite = IngredientNutrition(
            kcalPer100 = 900,
            proteinPer100 = 0.0,
            carbsPer100 = 0.0,
            fatPer100 = 100.0,
            grams = 100.0,
            microsPer100 = mapOf(vitC to 10.0),
            // Sem família na tabela, e por isso sem retenção nenhuma.
            retencoes = emptyMap(),
        )

        val r = RecipeCalc.compute(
            listOf(espinafres(100.0, mapOf(vitC to 0.5)), azeite),
            yieldGrams = 200.0,
        )

        // 100 g de espinafres dão 28 × 0,5 = 14 mg; o azeite dá os 10 inteiros. São 24 mg
        // em 200 g, ou seja 12 por 100 g. Um factor único de 0,5 no prato daria 9,5.
        assertEquals(12.0, r.microsPer100[vitC]!!, 1e-9)
    }

    /**
     * Os macros não têm retenção, e não é um esquecimento.
     *
     * A proteína não desaparece ao lume: muda de sítio. O que lhe acontece é a concentração,
     * e essa vem da base. Escrever-lhe um factor era inventar um número que nenhuma tabela
     * publica.
     */
    @Test
    fun `os macros so se concentram`() {
        val r = RecipeCalc.compute(
            listOf(espinafres(500.0, mapOf(vitC to 0.75))),
            yieldGrams = 400.0,
        )

        // 500 g a 2,9 g/100 g dão 14,5 g de proteína, agora em 400 g.
        assertEquals(14.5, r.totalProteinG, 1e-9)
        assertEquals(3.625, r.proteinPer100, 1e-9)
    }

    /**
     * Um nutriente sem factor publicado fica como está.
     *
     * É a diferença entre «não se perde» e «não se sabe»: a tabela do USDA cobre vitaminas e
     * minerais, e o selénio, o iodo e as vitaminas D, E e K não aparecem lá. Aplicar-lhes a
     * média dos outros era escrever um número que ninguém mediu.
     */
    @Test
    fun `nutriente sem factor publicado nao se toca`() {
        val comSelenio = espinafres(100.0, mapOf(vitC to 0.5)).copy(
            microsPer100 = mapOf(vitC to 28.0, "selenium_ug" to 1.0),
        )

        val r = RecipeCalc.compute(listOf(comSelenio), yieldGrams = 100.0)

        assertEquals(1.0, r.microsPer100["selenium_ug"]!!, 1e-9)
        assertEquals(14.0, r.microsPer100[vitC]!!, 1e-9)
    }

    /** O sódio tem campo próprio no resultado, e a retenção tem de o apanhar na mesma. */
    @Test
    fun `o sodio tambem perde o que a tabela diz`() {
        val caldo = IngredientNutrition(
            kcalPer100 = 10,
            proteinPer100 = 0.5,
            carbsPer100 = 1.0,
            fatPer100 = 0.1,
            grams = 100.0,
            sodiumMgPer100 = 400.0,
            retencoes = mapOf("sodium_mg" to 0.9),
        )

        val r = RecipeCalc.compute(listOf(caldo), yieldGrams = 100.0)

        assertEquals(360.0, r.sodiumMgPer100!!, 1e-9)
    }
}

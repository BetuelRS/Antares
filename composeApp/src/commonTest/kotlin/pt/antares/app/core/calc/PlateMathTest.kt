package pt.antares.app.core.calc

import pt.antares.app.core.model.UnitSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Que discos pôr na barra.
 *
 * O que estes testes defendem não é a aritmética — é que **os discos não se convertem**.
 * Quem levanta em libras tem discos de 45 lb, e não discos de 20 kg vestidos de 44,1 lb.
 * Converter o conjunto métrico daria uma lista de números que não existem em ginásio nenhum,
 * e a app diria à pessoa para pôr um disco que ela não tem.
 */
class PlateMathTest {

    @Test
    fun `sessenta e dois e meio em quilos sao vinte e um e um quarto por lado`() {
        val carga = PlateMath.paraOPeso(62.5, UnitSystem.METRIC)!!

        assertEquals(20.0, carga.barra)
        assertEquals(listOf(20.0, 1.25), carga.porLado)
        assertEquals(0.0, carga.sobra)
    }

    @Test
    fun `em libras a barra e os discos sao outros, e nao os metricos convertidos`() {
        val carga = PlateMath.paraOPeso(135.0, UnitSystem.IMPERIAL)!!

        assertEquals(45.0, carga.barra, "a barra olímpica em libras pesa 45, e não 44,1")
        assertEquals(listOf(45.0), carga.porLado)
        assertEquals(0.0, carga.sobra)
        assertTrue(
            carga.porLado.all { it in PlateMath.DISCOS_LB },
            "saiu um disco que não está no conjunto imperial: ${carga.porLado}",
        )
    }

    @Test
    fun `a barra sozinha nao leva discos nenhuns`() {
        val carga = PlateMath.paraOPeso(20.0, UnitSystem.METRIC)!!

        assertEquals(emptyList(), carga.porLado)
        assertEquals(0.0, carga.sobra)
    }

    @Test
    fun `abaixo do peso da barra nao ha calculo nenhum a fazer`() {
        // Não é zero discos: é uma pergunta sem resposta. Um exercício de 10 kg não se faz
        // com a barra olímpica, e desenhar uma barra vazia seria dizer que sim.
        assertNull(PlateMath.paraOPeso(10.0, UnitSystem.METRIC))
        assertNull(PlateMath.paraOPeso(19.9, UnitSystem.METRIC))
    }

    @Test
    fun `o que nao se consegue montar fica dito, e nao arredondado em silencio`() {
        // 61 kg: sobram 20,5 por lado, e o disco mais pequeno é 1,25.
        val carga = PlateMath.paraOPeso(61.0, UnitSystem.METRIC)!!

        assertEquals(listOf(20.0), carga.porLado)
        assertEquals(0.5, carga.sobra, "meio quilo por lado que nenhum disco faz")
    }

    @Test
    fun `os discos somam sempre o peso pedido menos a sobra`() {
        // A propriedade que interessa, sobre uma boa parte dos pesos que se usam a sério.
        var peso = 20.0
        while (peso <= 300.0) {
            val carga = PlateMath.paraOPeso(peso, UnitSystem.METRIC)!!
            val total = carga.barra + 2 * (carga.porLado.sum() + carga.sobra)
            assertEquals(peso, total, 0.001, "a conta não fecha em $peso kg")
            peso += 0.25
        }
    }

    @Test
    fun `nao se poe um disco que nao existe no conjunto`() {
        var peso = 20.0
        while (peso <= 300.0) {
            val carga = PlateMath.paraOPeso(peso, UnitSystem.METRIC)!!
            assertTrue(
                carga.porLado.all { it in PlateMath.DISCOS_KG },
                "saiu um disco fora do conjunto a $peso kg: ${carga.porLado}",
            )
            peso += 0.25
        }
    }

    @Test
    fun `os discos vem do maior para o menor`() {
        val carga = PlateMath.paraOPeso(157.5, UnitSystem.METRIC)!!

        assertEquals(carga.porLado.sortedDescending(), carga.porLado, "é a ordem de os enfiar")
    }
}

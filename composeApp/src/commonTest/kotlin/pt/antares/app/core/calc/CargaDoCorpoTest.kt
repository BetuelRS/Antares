package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A carga de um exercício de peso do corpo.
 *
 * O que estes testes defendem é que **o `weightKg` de uma série continua a querer dizer a
 * mesma coisa**: a carga total, em quilos. A `propostas/00` avisa que o peso do corpo daria
 * *«dois significados de `weightKg` para sempre»*, e daria — se ele passasse a guardar outra
 * coisa. Guarda o total; o que é novo é saber-se **quanto do total veio do corpo**.
 */
class CargaDoCorpoTest {

    @Test
    fun `sem carga adicional a serie pesa o corpo inteiro`() {
        val carga = CargaDoCorpo.calcular(pesoDoCorpoKg = 78.0, percentagem = 100, adicionalKg = 0.0)!!

        assertEquals(78.0, carga.totalKg)
        assertEquals(78.0, carga.doCorpoKg)
    }

    @Test
    fun `a carga adicional soma-se por cima do corpo`() {
        // Uma dominada com dez quilos no cinto.
        val carga = CargaDoCorpo.calcular(78.0, percentagem = 100, adicionalKg = 10.0)!!

        assertEquals(88.0, carga.totalKg)
        assertEquals(78.0, carga.doCorpoKg, "a parte do corpo não muda por se pôr um cinto")
    }

    @Test
    fun `a percentagem aplica-se so ao corpo e nao ao que se acrescenta`() {
        // Uma flexão a 65 %, com um disco de 5 kg nas costas: 50,7 + 5.
        val carga = CargaDoCorpo.calcular(78.0, percentagem = 65, adicionalKg = 5.0)!!

        assertEquals(55.7, carga.totalKg, 0.001)
        assertEquals(50.7, carga.doCorpoKg, 0.001)
    }

    @Test
    fun `sem peso registado nao ha carga nenhuma para calcular`() {
        // Não é zero: é uma pergunta sem resposta. Devolver zero fazia a app gravar uma
        // série de 0 kg, que é exactamente o que a validação recusa — e sem dizer porquê.
        assertNull(CargaDoCorpo.calcular(pesoDoCorpoKg = null, percentagem = 100, adicionalKg = 0.0))
    }

    @Test
    fun `uma percentagem fora do intervalo nao passa`() {
        assertNull(CargaDoCorpo.calcular(78.0, percentagem = 0, adicionalKg = 0.0))
        assertNull(CargaDoCorpo.calcular(78.0, percentagem = 201, adicionalKg = 0.0))
    }

    @Test
    fun `a percentagem por omissao e cem`() {
        assertEquals(100, CargaDoCorpo.PERCENTAGEM_POR_OMISSAO)
    }

    @Test
    fun `so os exercicios de peso do corpo contam o corpo`() {
        assertEquals(true, CargaDoCorpo.eDePesoDoCorpo("body only"))
        assertEquals(false, CargaDoCorpo.eDePesoDoCorpo("barbell"))
        assertEquals(false, CargaDoCorpo.eDePesoDoCorpo(null), "sem equipamento declarado não se assume nada")
    }
}

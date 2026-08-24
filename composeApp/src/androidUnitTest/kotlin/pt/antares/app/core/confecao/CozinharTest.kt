package pt.antares.app.core.confecao

import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A confeção: o que acontece à comida quando se cozinha.
 *
 * **A conta tem duas partes, e uma delas é a que toda a gente esquece.** Cozer espinafres
 * perde vitamina C para a água *e* perde água. Quem conta só a perda de vitamina fica com um
 * número mais errado do que quem não faz conta nenhuma, porque o que sobra fica mais
 * concentrado do que estava. É a divisão pelo rendimento, e é ela que estes testes cobram.
 *
 * A outra metade do trabalho é do `tools/confecao/`, que constrói a tabela a partir das duas
 * tabelas do USDA. Este ficheiro verifica a conta **e** que a tabela chegou inteira à app.
 */
class CozinharTest {

    private val cru = Por100g(
        kcal = 100.0,
        proteinG = 20.0,
        carbsG = 0.0,
        fatG = 2.0,
        micros = mapOf("vitB1_mg" to 1.0, "iron_mg" to 2.0, "selenium_ug" to 10.0),
    )

    private val grelhar = LinhaDeConfecao(
        familia = "vaca",
        metodo = "grelhado",
        rendimento = 0.8,
        retencoes = mapOf("vitB1_mg" to 0.75, "iron_mg" to 0.95),
    )

    @Test
    fun `o que sobra fica mais concentrado, e nao so mais pobre`() {
        val cozinhado = assertNotNull(cozinhar(cru, grelhar))

        // 100 g de cru dão 80 g de cozinhado. As mesmas 100 kcal ficam em 80 g, portanto
        // 125 kcal por 100 g — e quem só aplicasse a retenção obtinha 100.
        assertEquals(125.0, cozinhado.kcal, TOLERANCIA)
        assertEquals(25.0, cozinhado.proteinG, TOLERANCIA)

        // A tiamina perde um quarto e concentra-se um quinto: 1,0 × 0,75 ÷ 0,8 = 0,9375.
        assertEquals(0.9375, cozinhado.micros["vitB1_mg"]!!, TOLERANCIA)
    }

    @Test
    fun `um nutriente sem factor publicado fica como esta, e nao a zero`() {

        // O selénio não aparece na tabela de retenção do USDA. Assumir zero era dizer que
        // desaparece; assumir cem por cento seria dizer que não lhe acontece nada. A segunda
        // é falsa mas é a única que não inventa uma perda — e ainda leva a concentração.
        val cozinhado = assertNotNull(cozinhar(cru, grelhar))

        assertEquals(12.5, cozinhado.micros["selenium_ug"]!!, TOLERANCIA)
    }

    @Test
    fun `a balanca ganha a tabela`() {

        // A tabela é uma mediana de cortes que não são este. Quem pesou tem uma medição do
        // prato que está à sua frente, e é essa que vale.
        val pesado = assertNotNull(cozinhar(cru, grelhar, Pesagem(gramasCruas = 200.0, gramasCozinhadas = 100.0)))

        assertEquals(200.0, pesado.kcal, TOLERANCIA)
    }

    @Test
    fun `sem tabela e sem balanca nao se inventa um numero`() {
        val semRendimento = grelhar.copy(rendimento = null)

        assertNull(cozinhar(cru, semRendimento))

        // Com a balança, a mesma linha já responde: é isto que faz o ecrã pedir o peso em
        // vez de esconder o método.
        assertNotNull(cozinhar(cru, semRendimento, Pesagem(100.0, 70.0)))
    }

    @Test
    fun `uma pesagem impossivel nao passa por medicao`() {

        // Três vezes o peso não é uma cozedura, é um engano a escrever — e dividir por três
        // dava um terço da nutrição sem nada a assinalar o disparate.
        val disparate = Pesagem(gramasCruas = 100.0, gramasCozinhadas = 300.0)

        assertEquals(grelhar.rendimento, rendimentoDe(grelhar, disparate))
        assertNull(rendimentoDe(grelhar.copy(rendimento = null), disparate))
    }

    @Test
    fun `as gramas convertem-se nos dois sentidos`() {
        assertEquals(80.0, gramasDepoisDeCozinhar(100.0, grelhar)!!, TOLERANCIA)
        assertEquals(100.0, gramasAntesDeCozinhar(80.0, grelhar)!!, TOLERANCIA)
    }

    @Test
    fun `a tabela que a app transporta abre, e traz as duas fontes la dentro`() {
        val texto = File("src/commonMain/composeResources/files/confecao.json").readText()
        val tabela = Json { ignoreUnknownKeys = true }.decodeFromString<TabelaDeConfecao>(texto)

        assertTrue(tabela.linhas.size >= MINIMO_DE_LINHAS, "só ${tabela.linhas.size} linhas")
        assertTrue(tabela.metodos.size >= MINIMO_DE_METODOS, "só ${tabela.metodos.size} métodos")

        // Os rendimentos vêm da tabela de carne e aves, e só de lá. Se todas as linhas
        // ficarem sem rendimento, o oleoduto deixou de as juntar e ninguém dá por isso.
        val comRendimento = tabela.linhas.count { it.rendimento != null }
        assertTrue(comRendimento >= MINIMO_COM_RENDIMENTO, "só $comRendimento com rendimento")

        // E as retenções vêm da outra, que cobre muito mais do que carne.
        val familias = tabela.linhas.map { it.familia }.toSet()
        assertTrue("legumes" in familias, "a tabela perdeu os legumes")
        assertTrue("peixe" in familias, "a tabela perdeu o peixe")
    }

    @Test
    fun `um alimento sem familia nao tem metodo nenhum para oferecer`() {
        val texto = File("src/commonMain/composeResources/files/confecao.json").readText()
        val tabela = Json { ignoreUnknownKeys = true }.decodeFromString<TabelaDeConfecao>(texto)

        assertEquals(emptyList(), tabela.metodosDe(null))
        assertEquals(emptyList(), tabela.metodosDe("nao-existe"))
        assertTrue(tabela.metodosDe("vaca").isNotEmpty())
        assertNull(tabela.linha(null, "cozido"))
    }

    @Test
    fun `os numeros da tabela sao plausiveis`() {
        val texto = File("src/commonMain/composeResources/files/confecao.json").readText()
        val tabela = Json { ignoreUnknownKeys = true }.decodeFromString<TabelaDeConfecao>(texto)

        val rendimentosMaus = tabela.linhas
            .mapNotNull { l -> l.rendimento?.let { l to it } }
            .filter { (_, r) -> r <= 0.2 || r > 1.2 }
            .map { (l, r) -> "${l.familia}:${l.metodo}=$r" }
        assertEquals(emptyList(), rendimentosMaus, "rendimentos fora do que é possível cozinhar")

        val retencoesMas = tabela.linhas
            .flatMap { l -> l.retencoes.map { (k, v) -> Triple(l, k, v) } }
            .filter { (_, _, v) -> v < 0.0 || v > 1.0 }
            .map { (l, k, v) -> "${l.familia}:${l.metodo}:$k=$v" }
        assertEquals(emptyList(), retencoesMas, "retenções fora de 0 a 1 — um nutriente não se cria")
    }

    private companion object {
        const val TOLERANCIA = 0.0001

        // Contados a 2026-08-24: 59 pares família-método, 7 métodos, 17 com rendimento.
        const val MINIMO_DE_LINHAS = 40
        const val MINIMO_DE_METODOS = 5
        const val MINIMO_COM_RENDIMENTO = 10
    }
}

private fun assertEquals(esperado: Double, obtido: Double, tolerancia: Double) {
    assertTrue(abs(esperado - obtido) <= tolerancia, "esperava $esperado, veio $obtido")
}

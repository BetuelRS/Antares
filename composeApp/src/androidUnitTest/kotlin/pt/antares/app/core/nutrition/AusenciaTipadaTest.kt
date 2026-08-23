package pt.antares.app.core.nutrition

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Um estado não é um número, e um número não é um estado.
 *
 * A separação vive num sítio só — o [microsDeJson] devolve os números, o [estadosDeJson]
 * devolve o resto — e é isso que faz um vestígio não entrar nas somas do dia sem ninguém ter
 * de se lembrar disso em cada conta. Se o leitor deixar passar um estado como se fosse um
 * valor, **todas as somas da app passam a incluir um número que ninguém mediu**, e nenhuma
 * delas dá erro.
 *
 * O modo de falhar do outro lado é pior e já esteve a acontecer: até o leitor ser reescrito,
 * um único valor de texto no mapa fazia a leitura falhar **por inteiro**, e o alimento ficava
 * sem micronutriente nenhum. Sem erro, porque quem lê apanha a exceção e devolve um mapa
 * vazio — que é a coisa certa a fazer, e é por isso que este teste tem de existir.
 */
class AusenciaTipadaTest {

    private val comTudo = """
        {"vitC_mg":12.0,"selenium_ug":"<1.5","polyols_g":"vestigios","calcium_mg":250.0}
    """.trimIndent()

    @Test
    fun `os numeros passam e os estados nao`() {
        val medidos = microsDeJson(comTudo)

        assertEquals(mapOf("vitC_mg" to 12.0, "calcium_mg" to 250.0), medidos)
    }

    @Test
    fun `os estados leem-se, e cada um diz o que e`() {
        val estados = estadosDeJson(comTudo)

        assertEquals(2, estados.size, "não leu os dois estados: $estados")
        assertEquals(EstadoDeNutriente.AbaixoDoLimite(1.5), estados["selenium_ug"])
        assertEquals(EstadoDeNutriente.Vestigios, estados["polyols_g"])
    }

    @Test
    fun `um texto que nao seja estado nenhum nao vira valor`() {

        // Um catálogo mais recente pode escrever uma marca que esta versão da app não
        // conhece. O que não se pode é adivinhar: ignorar é a única resposta honesta, e é
        // preferível não mostrar um nutriente a mostrá-lo com um número inventado.
        val estranho = """{"vitC_mg":12.0,"iron_mg":"talvez","zinc_mg":"<0"}"""

        assertEquals(mapOf("vitC_mg" to 12.0), microsDeJson(estranho))
        assertEquals(emptyMap(), estadosDeJson(estranho))
    }

    @Test
    fun `escrever e voltar a ler devolve o mesmo`() {
        val medidos = mapOf("vitC_mg" to 12.0)
        val estados = mapOf(
            "selenium_ug" to EstadoDeNutriente.AbaixoDoLimite(1.5),
            "polyols_g" to EstadoDeNutriente.Vestigios,
        )

        val texto = microsParaJson(medidos, estados)

        assertEquals(medidos, microsDeJson(texto))
        assertEquals(estados, estadosDeJson(texto))
    }

    @Test
    fun `sem nada para escrever nao se escreve um mapa vazio`() {

        // Nulo e `{}` não são a mesma coisa para quem lê a base: o nulo é o que faz o ecrã
        // dizer porque é que o alimento não tem micronutrientes.
        assertEquals(null, microsParaJson(emptyMap(), emptyMap()))
    }

    @Test
    fun `o catalogo traz mesmo estados la dentro`() {

        // O leitor pode estar certo e o oleoduto ter deixado de os emitir — e aí nada
        // falha, apenas se perde outra vez a diferença entre «procurámos e não achámos» e
        // «ninguém analisou». Isto conta-os no ficheiro que a app transporta.
        val texto = File("src/commonMain/composeResources/files/catalogo.json").readText()

        val abaixo = Regex(""":"<[0-9]""").findAll(texto).count()
        val vestigios = Regex(""":"vestigios"""").findAll(texto).count()

        assertTrue(abaixo > MINIMO_ABAIXO, "só $abaixo «abaixo do limite» no catálogo")
        assertTrue(vestigios > MINIMO_VESTIGIOS, "só $vestigios «vestígios» no catálogo")
    }

    private companion object {
        // Contados no catálogo a 2026-08-23: 10 612 «abaixo do limite» e 792 vestígios.
        // São menos do que as células da CIQUAL — 11 286 e 1 214 — porque um estado não se
        // escreve onde já há número para a mesma chave, e porque 83 alimentos ficam de fora
        // do catálogo. Os mínimos são folgados de propósito: o que se quer apanhar é o
        // oleoduto deixar de os emitir, não uma variação da fonte.
        const val MINIMO_ABAIXO = 8_000
        const val MINIMO_VESTIGIOS = 600
    }
}

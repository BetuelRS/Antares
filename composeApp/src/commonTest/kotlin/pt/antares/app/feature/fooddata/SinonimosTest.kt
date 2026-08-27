package pt.antares.app.feature.fooddata

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Quem procura não escreve o nome do catálogo — escreve o nome que aprendeu em casa.
 *
 * Um brasileiro procura «abacaxi» e o catálogo diz «ananás». Um portista pede «cimbalino» e
 * a máquina serve um «café». Nos dois casos a app respondia que não tinha, e tem.
 *
 * O que aqui se protege sobretudo é o que **não** se acrescenta: um sinónimo a mais mistura
 * duas comidas diferentes na mesma resposta, e isso é pior do que não encontrar nada.
 */
class SinonimosTest {

    @Test
    fun `o abacaxi encontra o ananas`() {
        val texto = textoDePesquisa("Ananás", "Pineapple", null)
        assertTrue("abacaxi" in texto, "não tem abacaxi: $texto")
    }

    /** E ao contrário: a relação é simétrica, não é uma correcção de errado para certo. */
    @Test
    fun `o ananas encontra o abacaxi`() {
        val texto = textoDePesquisa("Abacaxi em calda", "", null)
        assertTrue("ananas" in texto, "não tem ananás: $texto")
    }

    @Test
    fun `o cafe tem os nomes todos que tem em Portugal`() {
        val texto = textoDePesquisa("Café expresso", "", null)
        for (nome in listOf("bica", "cimbalino")) {
            assertTrue(nome in texto, "falta «$nome» em: $texto")
        }
    }

    /** Um sinónimo dentro do nome conta: um «Sumo de ananás» procura-se por «abacaxi». */
    @Test
    fun `o sinonimo vale a meio do nome`() {
        val texto = textoDePesquisa("Sumo de ananás, sem açúcar", "", null)
        assertTrue("abacaxi" in texto)
        assertTrue("suco" in texto, "«sumo» e «suco» são a mesma coisa")
    }

    /**
     * Uma palavra só conta inteira.
     *
     * Sem isto, «pão» encontrava-se dentro de «Japão» e todos os pratos japoneses ganhavam
     * «bread» no índice — que é o mesmo erro silencioso que a composição de nomes fazia.
     */
    @Test
    fun `uma palavra dentro de outra nao conta`() {
        val texto = textoDePesquisa("Massa do Japão", "", null)
        assertTrue("bread" !in texto, "«Japão» não contém «pão»: $texto")
    }

    @Test
    fun `o ingles das tabelas encontra-se em portugues`() {
        val texto = textoDePesquisa("Chicken, breast, raw", "Chicken, breast, raw", null)
        assertTrue("frango" in texto, "quem escreve «frango» tem de encontrar isto")
        assertTrue("cru" in texto)
    }

    /** Uma palavra que já está no nome não se repete: o índice não cresce por nada. */
    @Test
    fun `um sinonimo que ja esta no nome nao se acrescenta`() {
        val texto = textoDePesquisa("Ananás e abacaxi", "", null)
        assertEquals(1, Regex("abacaxi").findAll(texto).count())
    }

    @Test
    fun `a esmagadora maioria dos alimentos nao ganha nada`() {
        assertTrue(sinonimosDe("bacalhau a bras").isEmpty())
        assertTrue(sinonimosDe("").isEmpty())
    }

    /**
     * Nenhum grupo tem uma palavra de outro.
     *
     * Duas palavras no mesmo grupo dizem-se a mesma comida. Se uma aparecesse em dois
     * grupos, os dois passavam a ser um só por transitividade — e um dia «laranja» acabava
     * a encontrar «café» sem ninguém perceber porquê.
     */
    @Test
    fun `nenhuma palavra vive em dois grupos`() {
        val vistas = mutableMapOf<String, Int>()
        val repetidas = mutableListOf<String>()

        for ((i, grupo) in SINONIMOS.withIndex()) {
            for (palavra in grupo) {
                val antes = vistas.put(palavra, i)
                if (antes != null && antes != i) repetidas += "«$palavra» está nos grupos $antes e $i"
            }
        }
        assertTrue(repetidas.isEmpty(), repetidas.joinToString("\n"))
    }

    /** Um grupo com uma palavra só não é um grupo — é uma linha que não faz nada. */
    @Test
    fun `todos os grupos tem pelo menos duas palavras`() {
        val pequenos = SINONIMOS.filter { it.size < 2 }
        assertTrue(pequenos.isEmpty(), "grupos com menos de duas palavras: $pequenos")
    }

    /**
     * Frutos diferentes não são sinónimos.
     *
     * A laranja e a tangerina são parecidas e têm composições diferentes. Juntá-las era
     * responder a uma pergunta com outra, e é a fronteira que esta lista não passa.
     */
    @Test
    fun `frutos parecidos nao se misturam`() {
        val texto = textoDePesquisa("Laranja", "", null)
        assertTrue("tangerina" !in texto)
        assertTrue("clementina" !in texto)
    }
}

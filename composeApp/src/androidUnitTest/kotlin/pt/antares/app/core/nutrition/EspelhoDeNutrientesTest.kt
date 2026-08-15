package pt.antares.app.core.nutrition

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * As chaves dos nutrientes estão escritas duas vezes: no [Nutrients] e no
 * `supabase/functions/_shared/nutrients.ts`, que é quem traduz os códigos da tabela
 * americana e os nomes do Open Food Facts antes de os mandar para o telemóvel.
 *
 * Escrever `vitB9_ug` num lado e `vitb9_ug` no outro não dá erro em lado nenhum. O servidor
 * grava a chave que inventou, o [Nutrients.normalize] não a reconhece, descarta-a em
 * silêncio, e o nutriente desaparece dos ecrãs de quem lê um código de barras. Este teste é
 * a única coisa que liga os dois ficheiros.
 */
class EspelhoDeNutrientesTest {

    private val deno = File("../supabase/functions/_shared/nutrients.ts").readText()

    // Apanha o lado direito de cada linha `1106: "vitA_ug",` e `"vitamin-a": "vitA_ug",`.
    private fun chavesDe(mapa: String): List<String> {
        val corpo = Regex("""$mapa[^{]*\{([^}]*)}""").find(deno)?.groupValues?.get(1)
            ?: error("o mapa $mapa desapareceu do nutrients.ts")
        return Regex(""":\s*"([^"]+)"""").findAll(corpo).map { it.groupValues[1] }.toList()
    }

    @Test
    fun `o ficheiro do servidor existe onde o teste o procura`() {
        assertTrue(
            deno.isNotBlank(),
            "sem o nutrients.ts não há nada a comparar, e o teste passaria a proteger nada",
        )
    }

    @Test
    fun `as chaves que o servidor escreve sao todas canonicas`() {
        val doServidor = (chavesDe("USDA_MICRO_IDS") + chavesDe("OFF_MICRO_KEYS")).toSortedSet()
        assertTrue(doServidor.size > 1, "os mapas do servidor não podem estar vazios")

        val desconhecidas = doServidor.filterNot { Nutrients.isCanonical(it) }
        assertEquals(
            emptyList(),
            desconhecidas,
            "estas chaves são gravadas pelo servidor e o Nutrients não as reconhece: os " +
                "valores chegam ao telemóvel e são deitados fora sem erro nenhum",
        )
    }

    @Test
    fun `nenhuma vitamina ou mineral fica sem traducao no servidor`() {
        // O sódio é a exceção declarada: o servidor manda-o no campo `sodiumMg` da tabela de
        // alimentos, como macro, e não no mapa de micros.
        val esperadas = (Nutrients.VITAMINS + Nutrients.MINERALS) - Nutrients.SODIUM

        val doServidor = (chavesDe("USDA_MICRO_IDS") + chavesDe("OFF_MICRO_KEYS")).toSet()
        val esquecidas = esperadas.filterNot { it in doServidor }
        assertEquals(
            emptyList(),
            esquecidas,
            "acrescentaste um nutriente ao Nutrients e não ao `_shared/nutrients.ts`: quem " +
                "lê um código de barras nunca o vai ver, porque o servidor não sabe " +
                "traduzir o código da tabela de origem para esta chave",
        )
    }

    @Test
    fun `o servidor converte as unidades pela terminacao da chave`() {
        // O `factorForKey` do lado Deno multiplica por mil ou por um milhão conforme o
        // sufixo. Uma chave canónica sem sufixo passaria por lá como grama e o valor vinha
        // mil vezes ao lado.
        val semSufixo = Nutrients.ALL.filterNot {
            it.endsWith("_ug") || it.endsWith("_mg") || it.endsWith("_g")
        }
        assertEquals(
            emptyList(),
            semSufixo,
            "a unidade vai colada à chave, e o `factorForKey` do servidor depende disso",
        )
    }
}

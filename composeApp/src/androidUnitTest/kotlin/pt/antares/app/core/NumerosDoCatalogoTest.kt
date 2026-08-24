package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Cobra a regra C4: um número do catálogo citado num documento reconta-se.
 *
 * Este repositório já foi esvaziado uma vez porque os documentos descreviam uma app que
 * não existia. Um total de alimentos é a forma mais silenciosa dessa mentira — não parte
 * nada, e quem o lê fica a acreditar.
 *
 * O `CHANGELOG.md` fica de fora de propósito: as entradas antigas descrevem o que era
 * verdade na versão delas, e reescrevê-las seria falsificar o histórico.
 */
class NumerosDoCatalogoTest {

    private val raiz = File("..")
    private val semente = File("src/commonMain/composeResources/files")

    /**
     * Cada origem, o padrão que a conta no catálogo, e o nome por que os documentos lhe
     * chamam. A origem lê-se do identificador porque é ele que a fixa: `ciqual-` vem da
     * ANSES, `tca-` do INSA, `pt-` são os extras escritos à mão e `ptx` os curados.
     *
     * A contagem é por expressão e não por leitura do JSON: são cinco megabytes, e lê-los
     * para um `size` seria desperdício.
     */
    private val origens = listOf(
        Origem("CIQUAL", Regex(""""id"\s*:\s*"ciqual-""")),
        Origem("USDA", Regex(""""id"\s*:\s*"usda-""")),
        Origem("INSA", Regex(""""id"\s*:\s*"tca-""")),
        Origem("PT_EXTRA", Regex(""""id"\s*:\s*"pt-""")),
    )

    private val documentos = listOf("README.md", "docs/referencia/dados-e-licencas.md")

    private data class Origem(val nome: String, val padrao: Regex)

    @Test
    fun `os numeros de alimentos citados batem com a semente`() {
        /**
         * Só a parte dos alimentos, e não o ficheiro todo.
         *
         * Desde que há fusões, o catálogo termina numa lista de **lápides** — o alimento que
         * saiu e o sucessor para onde quem o tinha deve seguir. Cada uma delas tem um `id`
         * com o mesmo formato, e contá-las dava sessenta e cinco alimentos que já não
         * existem. O número nos documentos ficava alto sem nada a acusá-lo, que é
         * exactamente o que este teste existe para impedir.
         */
        val catalogo = File(semente, "catalogo.json").readText().substringBefore("\"lapides\"")
        val contagens = origens.associateWith { origem -> origem.padrao.findAll(catalogo).count() }

        // Um catálogo vazio faria o teste passar por vacuidade: zero alimentos batem com
        // zero linhas citadas, e ninguém dava por isso.
        for ((origem, n) in contagens) {
            assertTrue(n > 0, "não se encontrou nenhum alimento de ${origem.nome} no catálogo")
        }

        val erros = mutableListOf<String>()

        for (caminho in documentos) {
            val doc = File(raiz, caminho)
            assertTrue(doc.exists(), "falta o documento $caminho")
            val linhas = doc.readLines()

            for ((origem, n) in contagens) {
                val queCitam = linhas.filter { it.contains(origem.nome) }
                if (queCitam.isEmpty()) continue

                // Basta uma linha certa: os documentos nomeiam a mesma origem em sítios
                // onde a contagem não vem a propósito, como a tabela de licenças.
                val bate = queCitam.any { linha ->
                    linha.contains(n.toString()) || linha.contains(comEspaco(n))
                }
                if (!bate) {
                    erros += "$caminho fala de ${origem.nome} e não diz $n — diz: " +
                        queCitam.first().trim().take(100)
                }
            }
        }

        assertTrue(
            erros.isEmpty(),
            "um número do catálogo mudou e o documento não:\n" + erros.joinToString("\n"),
        )
    }

    // Os documentos escrevem os milhares das duas maneiras, e as duas contam.
    private fun comEspaco(n: Int): String =
        n.toString().reversed().chunked(3).joinToString(" ").reversed()
}

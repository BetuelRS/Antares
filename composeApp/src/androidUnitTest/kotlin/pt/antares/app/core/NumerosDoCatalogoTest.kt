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
     * Cada origem, o padrão que a conta no catálogo, e as marcas por que os documentos lhe
     * chamam. A origem lê-se do identificador porque é ele que a fixa: `ciqual-` vem da
     * ANSES, `tca-` do INSA, `pt-` são os extras escritos à mão e `ptx` os curados.
     *
     * **As marcas incluem o próprio prefixo do identificador.** A 2.7.0 saiu com uma linha
     * que contava as origens pelo identificador e por mais nada — «3401 `ciqual-`, 2937
     * `usda-`, 1376 `tca-`» — e ficou quatro versões desactualizada sem este teste a ver
     * nada, porque não dizia CIQUAL nem USDA nem INSA em parte nenhuma.
     *
     * A contagem é por expressão e não por leitura do JSON: são cinco megabytes, e lê-los
     * para um `size` seria desperdício.
     */
    private val origens = listOf(
        Origem("CIQUAL", Regex(""""id"\s*:\s*"ciqual-"""), listOf("CIQUAL", "`ciqual-`")),
        Origem("USDA", Regex(""""id"\s*:\s*"usda-"""), listOf("USDA", "`usda-`")),
        Origem("INSA", Regex(""""id"\s*:\s*"tca-"""), listOf("INSA", "`tca-`")),
        Origem("curados", Regex(""""id"\s*:\s*"ptx"""), listOf("`ptx`")),
        Origem("extras à mão", Regex(""""id"\s*:\s*"pt-"""), listOf("`pt-`")),
    )

    private val documentos = listOf("README.md", "docs/referencia/dados-e-licencas.md")

    private data class Origem(val nome: String, val padrao: Regex, val marcas: List<String>)

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
                val queCitam = linhas.filter { linha -> origem.marcas.any { linha.contains(it) } }
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

                erros += contasColadas(queCitam, origem, n).map { "$caminho $it" }
            }
        }

        assertTrue(
            erros.isEmpty(),
            "um número do catálogo mudou e o documento não:\n" + erros.joinToString("\n"),
        )
    }

    /**
     * O mesmo, para o número que a **app** mostra a quem a abre.
     *
     * Um número errado num documento engana quem lê o repositório; este engana quem usa a
     * app. O ecrã de boas-vindas e o de atribuições prometiam 1376 alimentos do INSA depois
     * de quatro terem sido fundidos noutra medição — e ninguém o via, porque a promessa
     * está numa linha que não diz INSA.
     */
    @Test
    fun `o numero que a app promete bate com a semente`() {
        val catalogo = File(semente, "catalogo.json").readText().substringBefore("\"lapides\"")
        val insa = origens.first { it.nome == "INSA" }
        val quantos = insa.padrao.findAll(catalogo).count()
        assertTrue(quantos > 0, "não se encontrou nenhum alimento do INSA no catálogo")

        val textos = listOf(
            "src/commonMain/composeResources/values/strings.xml",
            "src/commonMain/composeResources/values-en/strings.xml",
        )

        // A promessa aparece em português e em inglês, e nas duas o número vem antes.
        val promessa = Regex("""(\d[\d ]*)\s+(?:alimentos medidos em Portugal|foods measured in Portugal)""")
        val erros = mutableListOf<String>()

        for (caminho in textos) {
            val ficheiro = File(caminho)
            assertTrue(ficheiro.exists(), "falta o ficheiro de textos $caminho")

            val encontradas = promessa.findAll(ficheiro.readText()).toList()
            assertTrue(encontradas.isNotEmpty(), "$caminho já não promete alimentos do INSA")

            for (m in encontradas) {
                val citado = m.groupValues[1].replace(" ", "").toInt()
                if (citado != quantos) {
                    erros += "$caminho promete $citado alimentos do INSA e são $quantos"
                }
            }
        }

        assertTrue(erros.isEmpty(), "a app promete um número que já não é verdade:\n" + erros.joinToString("\n"))
    }

    /**
     * Onde o documento escreve a conta colada à marca — «3401 `ciqual-`» —, **essa** tem de
     * ser a certa.
     *
     * Sem isto basta uma linha certa noutro sítio do mesmo ficheiro para o teste passar por
     * cima de uma enumeração inteira desactualizada, que foi como a linha das cinco origens
     * ficou quatro versões atrasada.
     */
    private fun contasColadas(linhas: List<String>, origem: Origem, n: Int): List<String> =
        origem.marcas.flatMap { marca ->
            val colada = Regex("""(\d[\d ]*)\s+${Regex.escape(marca)}""")
            linhas.flatMap { colada.findAll(it) }
                .map { it.groupValues[1].replace(" ", "").trim().toInt() }
                .filter { it != n }
                .map { "diz «$it $marca» e são $n" }
        }

    // Os documentos escrevem os milhares das duas maneiras, e as duas contam.
    private fun comEspaco(n: Int): String =
        n.toString().reversed().chunked(3).joinToString(" ").reversed()
}

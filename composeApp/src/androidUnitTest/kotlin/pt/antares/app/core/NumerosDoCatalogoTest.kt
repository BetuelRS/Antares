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
     * Cada origem, o padrão que a conta na semente, e o nome por que os documentos lhe
     * chamam. A contagem é por expressão e não por leitura do JSON: os dois ficheiros têm
     * formatações diferentes — um compacto, outro indentado — e quatro megabytes lidos
     * para um `size` seriam desperdício.
     */
    private val origens = listOf(
        Origem("CIQUAL", "seed_foods.json", Regex(""""id"\s*:\s*"ciqual-""")),
        Origem("USDA", "seed_foods.json", Regex(""""id"\s*:\s*"usda-""")),
        Origem("INSA", "seed_foods_tca.json", Regex(""""id"\s*:\s*"tca-""")),
        Origem("PT_EXTRA", "seed_foods.json", Regex(""""origin"\s*:\s*"PT_EXTRA"""")),
    )

    private val documentos = listOf("README.md", "docs/referencia/dados-e-licencas.md")

    private data class Origem(val nome: String, val ficheiro: String, val padrao: Regex)

    @Test
    fun `os numeros de alimentos citados batem com a semente`() {
        val contagens = origens.associateWith { origem ->
            val texto = File(semente, origem.ficheiro).readText()
            origem.padrao.findAll(texto).count()
        }

        // Uma semente vazia faria o teste passar por vacuidade: zero alimentos batem com
        // zero linhas citadas, e ninguém dava por isso.
        for ((origem, n) in contagens) {
            assertTrue(n > 0, "não se encontrou nenhum alimento de ${origem.nome} na semente")
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

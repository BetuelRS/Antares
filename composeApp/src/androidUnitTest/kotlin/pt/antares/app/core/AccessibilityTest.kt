package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AccessibilityTest {

    private fun fontes(): List<File> =
        File("src/commonMain/kotlin/pt/antares/app")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    @Test
    fun `nenhum botao de icone fica sem se apresentar`() {

        val padrao = Regex(
            """IconButton\([^)]*\)\s*\{[^}]*?contentDescription\s*=\s*null""",
            RegexOption.DOT_MATCHES_ALL,
        )
        val infratores = fontes().filter { padrao.containsMatchIn(it.readText()) }.map { it.name }

        assertTrue(
            infratores.isEmpty(),
            "botões de ícone sem descrição — o TalkBack lê só \"botão\": $infratores",
        )
    }

    @Test
    fun `os elementos tocaveis dizem que sao tocaveis`() {

        val permitidos = setOf("SupernovaCelebration.kt")
        val semPapel = mutableListOf<String>()

        for (ficheiro in fontes()) {
            if (ficheiro.name in permitidos) continue
            val linhas = ficheiro.readText().split("\n")
            linhas.forEachIndexed { i, linha ->
                if (!linha.contains(".clickable(")) return@forEachIndexed

                val contexto = linhas.subList(
                    (i - 4).coerceAtLeast(0),
                    (i + 6).coerceAtMost(linhas.size),
                ).joinToString("\n")
                val anunciado = contexto.contains("role =") ||
                    contexto.contains("semantics") ||
                    contexto.contains("contentDescription")
                if (!anunciado) semPapel += "${ficheiro.name}:${i + 1}"
            }
        }

        assertTrue(
            semPapel.isEmpty(),
            "tocáveis que o TalkBack não anuncia como tal — junta " +
                "`role = Role.Button` (ou uma descrição, se o texto não chegar):\n" +
                semPapel.joinToString("\n"),
        )
    }

    /**
     * Um `contentDescription = null` é uma decisão: diz que aquele elemento não acrescenta
     * nada ao que já está escrito ao lado. Escrito sem razão, é indistinguível de esquecimento
     * — e era assim que estavam os dezassete que havia.
     *
     * O teste não sabe julgar se a decisão está certa. Sabe exigir que ela esteja escrita.
     */
    @Test
    fun `um nulo decorativo tem de dizer porque e que e decorativo`() {
        val semRazao = mutableListOf<String>()

        for (ficheiro in fontes()) {
            val linhas = ficheiro.readText().split("\n")
            linhas.forEachIndexed { i, linha ->
                if (!linha.contains("contentDescription = null")) return@forEachIndexed

                // O comentário fica nas linhas de cima, que é onde a convenção da app o põe.
                val acima = linhas.subList((i - LINHAS_DE_RAZAO).coerceAtLeast(0), i)
                if (acima.none { it.trimStart().startsWith("//") }) {
                    semRazao += "${ficheiro.name}:${i + 1}"
                }
            }
        }

        assertTrue(
            semRazao.isEmpty(),
            "`contentDescription = null` sem razão escrita. Se for decorativo, diz porquê; " +
                "se não for, dá-lhe uma descrição:\n" + semRazao.joinToString("\n"),
        )
    }

    /**
     * As imagens e os gráficos são conteúdo, e o teste antigo não olhava para eles: só via
     * botões de ícone. Uma foto de progresso, a imagem de um exercício ou um gráfico sem
     * descrição são um buraco silencioso no meio do ecrã.
     */
    @Test
    fun `imagens e graficos apresentam-se`() {
        // A fronteira `\b` importa: sem ela, um `PickedImage(` passa por uma imagem.
        val padrao = Regex("""\b(AsyncImage|Image|Canvas)\(""")
        val semDescricao = mutableListOf<String>()

        for (ficheiro in fontes()) {
            val linhas = ficheiro.readText().split("\n")
            linhas.forEachIndexed { i, linha ->
                if (!padrao.containsMatchIn(linha) || linha.trimStart().startsWith("import ")) {
                    return@forEachIndexed
                }

                // Ou o bloco diz o que a imagem mostra, ou as linhas de cima dizem porque é
                // que ela não mostra nada — a mesma regra do `contentDescription = null`.
                val bloco = linhas.subList(i, (i + LINHAS_DE_BLOCO).coerceAtMost(linhas.size))
                    .joinToString("\n")
                val acima = linhas.subList((i - LINHAS_DE_RAZAO).coerceAtLeast(0), i)

                val anunciado = "contentDescription" in bloco ||
                    "semantics" in bloco ||
                    acima.any { it.trimStart().startsWith("//") }
                if (!anunciado) semDescricao += "${ficheiro.name}:${i + 1}"
            }
        }

        assertTrue(
            semDescricao.isEmpty(),
            "imagens ou gráficos que o TalkBack não anuncia — dá-lhes uma descrição, ou um " +
                "`contentDescription = null` com a razão escrita:\n" + semDescricao.joinToString("\n"),
        )
    }

    private companion object {
        // A razão cabe em três linhas de comentário; mais do que isso já é outra coisa.
        const val LINHAS_DE_RAZAO = 3

        // Uma chamada a desenhar uma imagem não passa daqui antes de fechar os parâmetros.
        const val LINHAS_DE_BLOCO = 12
    }
}

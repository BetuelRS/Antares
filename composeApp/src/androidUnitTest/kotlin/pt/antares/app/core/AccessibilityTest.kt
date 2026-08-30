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

    /**
     * Uma descrição escrita no código não se traduz e não se revê: o leitor de ecrã anuncia
     * a mesma palavra nos dois idiomas.
     *
     * Havia duas — `"-5"` e `"+5"`, no ecrã de registar exercício —, e os outros quatro
     * testes deixavam-nas passar por olharem só para o `null` e para a ausência. O TalkBack
     * dizia «menos cinco», sem dizer de quê.
     */
    @Test
    fun `nenhuma descricao de conteudo esta escrita no codigo`() {
        val padrao = Regex("""contentDescription\s*=\s*"""")
        val literais = mutableListOf<String>()

        for (ficheiro in fontes()) {
            ficheiro.readText().split("\n").forEachIndexed { i, linha ->
                if (padrao.containsMatchIn(linha)) literais += "${ficheiro.name}:${i + 1}"
            }
        }

        assertTrue(
            literais.isEmpty(),
            "descrições escritas no código — tira-as para `strings.xml`, senão o TalkBack " +
                "anuncia a mesma palavra nos dois idiomas:\n" + literais.joinToString("\n"),
        )
    }

    /**
     * Uma largura fixa é uma aposta sobre quanto espaço o texto vai ocupar, e quem escolhe o
     * tamanho da letra é quem usa a app — não quem a escreve.
     *
     * O `estudo/transversal/03-acessibilidade.md` §3.1 nomeia o caso: *«três campos de largura
     * fixa lado a lado — a 200 % os rótulos não cabem»*. Aconteceu na 2.19.0, três vezes na
     * mesma linha: um botão fora do ecrã, um atalho partido ao meio, e outro a ler-se na
     * vertical. Nenhum dos 1667 testes o via.
     *
     * **Tentou-se medir isto a correr, e não dá:** um teste de Robolectric com `fontScale = 2`
     * passa na mesma sobre o código partido, porque o Robolectric mede o texto «15» a três
     * pixels — sem fontes a sério nada transborda. Provado a repor a forma partida de
     * propósito.
     *
     * Fica então a mesma forma do `contentDescription = null`: **não se proíbe, exige-se a
     * razão escrita**. Um `Spacer` não conta — mede um vão, não segura texto.
     */
    @Test
    fun `uma largura fixa tem de dizer porque e que e fixa`() {
        val semRazao = mutableListOf<String>()

        for (ficheiro in fontes()) {
            val linhas = ficheiro.readText().split("\n")
            linhas.forEachIndexed { i, linha ->
                if (!linha.contains(".width(") || linha.contains("Spacer(")) return@forEachIndexed

                val acima = linhas.subList((i - LINHAS_DE_RAZAO).coerceAtLeast(0), i)
                if (acima.none { it.trimStart().startsWith("//") }) {
                    semRazao += "${ficheiro.name}:${i + 1}"
                }
            }
        }

        assertTrue(
            semRazao.isEmpty(),
            "larguras fixas sem razão escrita. Diz porque é que aquela largura é fixa — e se " +
                "for uma que ainda não aguenta letra grande, diz isso e diz de quem é:\n" +
                semRazao.joinToString("\n"),
        )
    }

    private companion object {
        // A razão cabe em três linhas de comentário; mais do que isso já é outra coisa.
        const val LINHAS_DE_RAZAO = 3

        // Uma chamada a desenhar uma imagem não passa daqui antes de fechar os parâmetros.
        const val LINHAS_DE_BLOCO = 12
    }
}

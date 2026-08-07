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
}

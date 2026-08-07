package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DeadCodeSweepTest {

    private val excecoes = mapOf(

        "finishedSessions" to "lida pelos testes de jejum, para verificar o que ficou gravado",
        "countForRun" to "lida pelos testes de corrida, para provar a poda dos pontos",
        "deleteForRun" to "usada pela poda dentro do próprio DAO e verificada em teste",
        "observeRoutineForDay" to "lida pelos testes do plano semanal",
        "unlockedCount" to "lida pelos testes das conquistas",
        "sujosDeDemo" to
            "lida pelo DemoDataWriterTest: pergunta à base se alguma linha de " +
            "demonstração ficou marcada para sincronizar. O motor promete que não; " +
            "isto verifica-o na base, que é a diferença entre provar e acreditar",
    )

    private fun fontesDaApp(): List<File> =
        listOf("src/commonMain/kotlin", "src/androidMain/kotlin")
            .map(::File)
            .filter { it.exists() }
            .flatMap { it.walkTopDown().filter { f -> f.isFile && f.extension == "kt" } }

    @Test
    fun `nenhum motor tem funcao publica sem quem a chame`() {
        val fontes = fontesDaApp()
        val corpo = fontes.joinToString("\n") { it.readText() }

        val motores = fontes.filter {
            Regex("Repository\\.kt$|Calc\\.kt$|Daos?\\.kt$").containsMatchIn(it.name)
        }

        val orfas = mutableListOf<String>()
        for (ficheiro in motores) {
            val declaracoes = Regex("""^\s{0,8}(?:suspend )?fun (\w+)\s*\(""", RegexOption.MULTILINE)
                .findAll(ficheiro.readText())
                .map { it.groupValues[1] }
            for (nome in declaracoes) {
                if (nome in excecoes) continue
                val chamadas = Regex("""[.\s(]$nome\s*\(""").findAll(corpo).count()
                val declaracoesDoNome = Regex("""fun $nome\s*\(""").findAll(corpo).count()
                if (chamadas <= declaracoesDoNome) orfas += "${ficheiro.name}: $nome"
            }
        }

        assertTrue(
            orfas.isEmpty(),
            "Funções de motor que ninguém chama. Cada uma é uma de três coisas: " +
                "código a apagar, uma peça por ligar a um ecrã, ou uma exceção com " +
                "razão escrita neste teste. Nunca a quarta — deixar estar:\n" +
                orfas.joinToString("\n"),
        )
    }

    @Test
    fun `toda a excecao traz a razao por escrito`() {
        val semRazao = excecoes.filterValues { it.isBlank() }.keys
        assertTrue(semRazao.isEmpty(), "exceções sem razão: $semRazao")
    }
}

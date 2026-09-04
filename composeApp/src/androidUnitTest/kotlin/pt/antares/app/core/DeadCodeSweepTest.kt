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
        "indicesDaTabela" to
            "lida pelo teste-guarda do índice em `key`. Existe porque a alternativa era " +
            "comparar relógios, e uma comparação de relógio num servidor partilhado mede a " +
            "carga tanto quanto mede o código — esse teste ficou vermelho duas vezes por isso",
        "current" to
            "a sequência estrita do `LoggingStreak`, sem perdões. **Hoje não tem chamador na " +
            "app**: o ecrã do Hoje usa a `currentWithFreeze` e a `longest`, e o comentário " +
            "dela dizia que era a dos marcos e dos troféus — que era verdade quando foi " +
            "escrito e deixou de ser. Fica como excepção e não se apaga porque o " +
            "`estudo/motor/07` trata as duas sequências como uma decisão de produto («estrita " +
            "para os troféus, perdoada para o ecrã»), e escolher entre ligar uma e apagar a " +
            "outra é do dono. Medido a 2026-09-05, ao alargar esta varredura ao `core/calc/`",
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

        // O `core/calc/` inteiro, e não só os ficheiros que acabam em `Calc.kt`.
        //
        // A varredura nasceu com uma convenção de nomes que a pasta já tinha abandonado:
        // **oito dos quarenta e um ficheiros** do motor acabam em `Calc.kt`, e os outros
        // trinta e três — `WeightTrend`, `ProteinFloor`, `NavyUncertainty`, `PlateMath` — não
        // eram olhados por ninguém. Medido a 2026-09-05, depois de a varredura deixar passar
        // uma função escrita nesse dia.
        val motores = fontes.filter {
            Regex("Repository\\.kt$|Calc\\.kt$|Daos?\\.kt$").containsMatchIn(it.name) ||
                it.invariantSeparatorsPath.contains("/core/calc/")
        }

        val orfas = mutableListOf<String>()
        for (ficheiro in motores) {
            val declaracoes = Regex("""^\s{0,8}(?:suspend )?fun (\w+)\s*\(""", RegexOption.MULTILINE)
                .findAll(ficheiro.readText())
                .map { it.groupValues[1] }
            for (nome in declaracoes) {
                if (nome in excecoes) continue
                // `[({]` e não só `(`: uma função cujo último parâmetro é uma lambda chama-se
                // `avgMacro { … }`, sem parêntesis nenhuns. Exigir o parêntesis fazia a
                // varredura acusar de morta uma função chamada três linhas abaixo.
                val chamadas = Regex("""[.\s(]$nome\s*[({]""").findAll(corpo).count()
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

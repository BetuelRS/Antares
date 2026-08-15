package pt.antares.app.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O grafo de navegação está partido por área, e isso abriu uma maneira nova de o partir a
 * sério: uma rota declarada sem ecrã, ou um ficheiro de área esquecido no `AntaresNavHost`.
 *
 * Nenhuma das duas dá erro de compilação. A rota existe, navega-se para ela, e a app fica num
 * ecrã em branco — só se descobre a usar.
 */
class TodaRotaTemEcraTest {

    private val pasta = File("src/commonMain/kotlin/pt/antares/app/navigation")

    private val rotas = File(pasta, "Routes.kt").readText()
    private val navHost = File(pasta, "AntaresNavHost.kt").readText()

    private val ficheirosDeArea = pasta.listFiles()
        .orEmpty()
        .filter { it.name.startsWith("RotasD") }

    private val todasAsAreas = ficheirosDeArea.joinToString("\n") { it.readText() }

    private fun rotasDeclaradas(): Set<String> =
        Regex("""data (?:object|class) (\w+)\s*[(:]""").findAll(rotas)
            .map { it.groupValues[1] }
            .toSet()

    @Test
    fun `ha rotas declaradas para encontrar`() {
        assertTrue(
            rotasDeclaradas().size > 40,
            "o regex deixou de casar, e um teste que não encontra nada passa sempre",
        )
    }

    @Test
    fun `toda a rota declarada tem um ecra`() {
        val semEcra = rotasDeclaradas().filterNot { nome ->
            todasAsAreas.contains("composable<Route.$nome>")
        }
        assertEquals(
            emptyList(),
            semEcra,
            "estas rotas existem e não levam a lado nenhum: navega-se para elas e fica um " +
                "ecrã em branco, sem erro nenhum a avisar",
        )
    }

    @Test
    fun `todo o ficheiro de area e chamado pelo NavHost`() {
        val funcoes = Regex("""internal fun NavGraphBuilder\.(\w+)\(""").findAll(todasAsAreas)
            .map { it.groupValues[1] }
            .toList()
        assertTrue(funcoes.size >= ficheirosDeArea.size, "cada ficheiro de área declara a sua função")

        val esquecidas = funcoes.filterNot { navHost.contains("$it(navController)") }
        assertEquals(
            emptyList(),
            esquecidas,
            "um ficheiro de rotas que ninguém chama é um punhado de ecrãs inalcançáveis",
        )
    }

    @Test
    fun `nenhuma rota esta registada duas vezes`() {
        val registadas = Regex("""composable<Route\.(\w+)>""").findAll(todasAsAreas)
            .map { it.groupValues[1] }
            .toList()
        val repetidas = registadas.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertEquals(
            emptySet(),
            repetidas,
            "a mesma rota em duas áreas: uma delas ganha, e qual é depende da ordem de " +
                "registo — que ninguém está a olhar",
        )
    }
}

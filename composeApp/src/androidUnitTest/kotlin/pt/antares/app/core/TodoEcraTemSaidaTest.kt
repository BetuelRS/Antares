package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Um ecrã que não é separador tem de ter saída escrita.
 *
 * Nasceu de um defeito da **2.20.1**, e de um que nenhum dos 1684 testes via: a corrida era
 * separador, e a barra de baixo — que está sempre no ecrã de um separador — era a única
 * maneira de sair dela. Ao deixar de ser separador, passou a ser empurrada a partir do painel
 * de treino, **a barra deixou de aparecer, e o ecrã ficou sem porta de saída nenhuma**. Só o
 * gesto do sistema o tirava de lá, e um gesto não se vê.
 *
 * O que se verifica é o mínimo que se consegue verificar a partir do código: o bloco de cada
 * rota que não é separador tem de conter um `popBackStack` ou um `popUpTo`. Não prova que a
 * seta está desenhada — prova que alguém pensou na saída, que é onde este defeito nasceu.
 */
class TodoEcraTemSaidaTest {

    private val pasta = File("src/commonMain/kotlin/pt/antares/app/navigation")

    private val separadores: List<String> = pasta.resolve("Routes.kt").readText()
        .substringAfter("val bottomBarRoutes")
        .substringAfter("(")
        .substringBefore(")")
        .split(",")
        .mapNotNull { it.trim().removePrefix("Route.").ifBlank { null } }

    /**
     * Recorta o bloco de cada `composable<Route.X>` contando chavetas. Um `indexOf("}")` dava
     * o fim do primeiro lambda lá dentro, que é quase sempre a primeira linha do ecrã.
     */
    private fun blocos(texto: String): List<Pair<String, String>> =
        Regex("""composable<Route\.(\w+)>[^\n]*\{""").findAll(texto).map { m ->
            val inicio = m.range.last
            m.groupValues[1] to texto.substring(inicio, fimDoBloco(texto, inicio) + 1)
        }.toList()

    /** O índice da chaveta que fecha a que está em [inicio]. */
    private fun fimDoBloco(texto: String, inicio: Int): Int {
        var profundidade = 0
        for (i in inicio until texto.length) {
            if (texto[i] == '{') profundidade++
            if (texto[i] == '}') profundidade--
            if (profundidade == 0 && texto[i] == '}') return i
        }
        return texto.length - 1
    }

    @Test
    fun `os cinco separadores lem-se do codigo e nao estao escritos aqui`() {
        assertTrue(
            separadores.size == 5,
            "os separadores deixaram de ser cinco: $separadores — decidir, e não ajustar o teste",
        )
    }

    @Test
    fun `nenhuma rota fora da barra fica sem saida`() {
        val sem = pasta.listFiles()
            .orEmpty()
            .filter { it.extension == "kt" }
            .flatMap { ficheiro -> blocos(ficheiro.readText()).map { ficheiro.name to it } }
            .filter { (_, rota) -> rota.first !in separadores }
            .filter { (_, rota) -> !rota.second.contains("popBackStack") }
            .filter { (_, rota) -> !rota.second.contains("popUpTo") }
            .map { (ficheiro, rota) -> "$ficheiro :: ${rota.first}" }

        assertTrue(
            sem.isEmpty(),
            "rotas empurradas sem saída escrita — a barra de baixo não aparece nelas: $sem",
        )
    }
}

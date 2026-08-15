package pt.antares.app.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Um ecrã de lista sem estado vazio é um ecrã em branco no primeiro dia de quem instala a app.
 *
 * Não é uma falha de acabamento: é a causa-raiz de sintomas que pareciam separados — o
 * Progresso com «1% dos dias», o resumo de corrida com dois terços de nada, o onboarding com
 * 60% de vazio. Todos são o mesmo buraco visto de ângulos diferentes.
 *
 * O padrão é sempre o mesmo — ícone, uma frase do que falta, e o botão do próximo passo — e
 * mora no `EmptyState`. Quem não o usa tem de estar nesta lista, com a razão escrita.
 */
class EcraVazioTemEstadoTest {

    private val raiz = File("src/commonMain/kotlin/pt/antares/app/feature")

    /**
     * Listas que **não podem estar vazias**, e por isso não têm o que dizer no vazio.
     *
     * Cada uma é uma decisão, não uma dispensa: a lista é construída em código ou vem de um
     * ficheiro que a app traz consigo, e um estado vazio ali era uma frase que ninguém lê.
     */
    private val naoPodemEstarVazias = mapOf(
        "AboutScreen.kt" to "o changelog vem compilado com a app; sem versões não há app",
        "WorkoutDetailScreen.kt" to
            "um treino guardado tem sempre exercícios — sem nenhum não chega a ser gravado",
        "NutritionStatsScreen.kt" to
            "tem estado vazio próprio, com o texto `stat_no_data`, e não pelo EmptyState: " +
                "o ecrã é uma lista de grupos e o vazio aparece dentro dela",
    )

    /**
     * Há três maneiras honestas de tratar uma lista vazia, e as três contam:
     *
     * - o `EmptyState`, que é o padrão — ícone, frase, botão do próximo passo;
     * - um texto próprio, quando o vazio vive dentro de uma lista maior e um ecrã inteiro
     *   centrado ficaria a flutuar no meio de outras secções;
     * - não desenhar a lista de todo, que é o certo para uma tira de imagens ou um resumo
     *   que só faz sentido com conteúdo.
     *
     * O que não conta é não haver ramo nenhum para o caso de estar vazia.
     */
    private fun trataOVazio(fonte: String): Boolean =
        "EmptyState(" in fonte ||
            Regex("""_empty|empty_|_no_data|_no_\w+""").containsMatchIn(fonte) ||
            Regex("""\.isEmpty\(\)|\.isNotEmpty\(\)""").containsMatchIn(fonte)

    private fun ecrasComLista(): List<File> = raiz.walkTopDown()
        .filter { it.isFile && (it.name.endsWith("Screen.kt") || it.name.endsWith("Sheet.kt")) }
        .filter { f ->
            val s = f.readText()
            Regex("""\bitems\(|\bitemsIndexed\(""").containsMatchIn(s)
        }
        .toList()

    @Test
    fun `ha ecras de lista para verificar`() {
        assertTrue(
            ecrasComLista().size > 10,
            "o teste deixou de encontrar ecrãs, e um teste que não encontra nada passa sempre",
        )
    }

    @Test
    fun `todo o ecra de lista diz alguma coisa quando esta vazio`() {
        val semEstado = ecrasComLista()
            .filterNot { it.name in naoPodemEstarVazias }
            .filterNot { f -> trataOVazio(f.readText()) }
            .map { it.name }
            .sorted()

        assertEquals(
            emptyList(),
            semEstado,
            "estes ecrãs mostram uma lista e não dizem nada quando ela está vazia. No " +
                "primeiro dia de quem instala a app são um ecrã em branco",
        )
    }

    @Test
    fun `as excecoes existem todas`() {
        val nomes = raiz.walkTopDown().filter { it.isFile }.map { it.name }.toSet()
        val fantasmas = naoPodemEstarVazias.keys.filterNot { it in nomes }
        assertEquals(
            emptyList(),
            fantasmas,
            "uma exceção para um ficheiro que já não existe é uma dispensa que ninguém " +
                "voltou a olhar",
        )
    }
}

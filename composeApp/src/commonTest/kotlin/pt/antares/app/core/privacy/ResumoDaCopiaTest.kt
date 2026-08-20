package pt.antares.app.core.privacy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O resumo é o que alguém lê antes de carregar em «substituir», que apaga tudo o que está na
 * app. Tem de dizer a verdade sobre um ficheiro que veio de fora — e, quando não a souber,
 * tem de dizer que não sabe em vez de inventar um zero.
 */
class ResumoDaCopiaTest {

    private val copia = """
        {
          "exportadoEm": "2024-03-11T09:12:44Z",
          "versaoApp": "1.4.0",
          "nota": "Dados pessoais exportados do Antares.",
          "food_log": [{"id": 1}, {"id": 2}, {"id": 3}],
          "weight_log": [{"id": 1}],
          "run": []
        }
    """.trimIndent()

    @Test
    fun `le a data e a versao que escreveu o ficheiro`() {
        val resumo = LeitorDeResumo.ler(copia)
        assertEquals("2024-03-11T09:12:44Z", resumo?.exportadoEm)
        assertEquals("1.4.0", resumo?.versaoApp)
    }

    @Test
    fun `conta as linhas de cada tabela`() {
        val resumo = LeitorDeResumo.ler(copia)
        assertEquals(3, resumo?.contagens?.get("food_log"))
        assertEquals(1, resumo?.contagens?.get("weight_log"))
        assertEquals(0, resumo?.contagens?.get("run"))
        assertEquals(4, resumo?.total)
    }

    @Test
    fun `os campos do cabecalho nao viram tabelas`() {

        // «nota», «exportadoEm» e «versaoApp» são texto, não listas. Se entrassem nas
        // contagens, o ecrã dizia que a cópia traz três tabelas que não existem.
        val chaves = LeitorDeResumo.ler(copia)?.contagens?.keys.orEmpty()
        assertEquals(setOf("food_log", "weight_log", "run"), chaves)
    }

    @Test
    fun `um ficheiro que nao e um JSON devolve nulo`() {
        assertNull(LeitorDeResumo.ler("isto não é um ficheiro de cópia"))
        assertNull(LeitorDeResumo.ler(""))
    }

    @Test
    fun `uma lista no topo devolve nulo`() {

        // Um JSON válido que não é um objeto. Sem esta guarda a leitura rebentava em vez de
        // devolver nulo, e o diálogo de importação nunca chegava a abrir.
        assertNull(LeitorDeResumo.ler("""[1, 2, 3]"""))
    }

    @Test
    fun `uma copia sem cabecalho ainda conta as tabelas`() {

        // Cópias antigas podem não trazer os campos do cabeçalho. O ecrã diz que não sabe a
        // data — e continua a dizer quantos registos lá estão, que é a parte que mais decide.
        val resumo = LeitorDeResumo.ler("""{"food_log": [{"id": 1}, {"id": 2}]}""")
        assertNull(resumo?.exportadoEm)
        assertEquals(2, resumo?.total)
    }

    @Test
    fun `um exportadoEm nulo le-se como desconhecido`() {

        // O exportador escreve os valores por omissão, e um nulo escrito chegava aqui como o
        // texto «null» — que o ecrã mostraria tal e qual, ao lado da palavra «Exportado em».
        val resumo = LeitorDeResumo.ler("""{"exportadoEm": null, "food_log": []}""")
        assertNull(resumo?.exportadoEm)
        assertTrue(resumo != null)
    }
}

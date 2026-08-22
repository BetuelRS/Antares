package pt.antares.app.core.nutrition

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Há um vocabulário de nutrientes, e é um só.
 *
 * Um nutriente escrito de duas maneiras é dois nutrientes, e ninguém dá por isso: a app mostra
 * os dois, cada um com metade dos alimentos, e a barra de cada um fica a meio sem razão
 * nenhuma. Não dá erro, não aparece em lado nenhum, e quem olha para o ecrã conclui que come
 * mal.
 *
 * O vocabulário vive em `tools/catalogo/vocabulario.csv`, e é ele que o oleoduto cobra: uma
 * chave emitida e não declarada chumba a construção. Este teste fecha a outra ponta — **a app
 * e o oleoduto têm de conhecer as mesmas chaves.** Sem ele, um nutriente novo entra no
 * catálogo e a app não sabe o que fazer com ele, ou sai do catálogo e a app continua a
 * perguntar por ele.
 *
 * As referências diárias vivem em `seed_efsa_drv.csv`, que é o que a app lê. Estão repetidas
 * no vocabulário para quem lá vai ver, e é isso que as faz poder discordar — daí a terceira
 * verificação. Ao escrever o vocabulário pela primeira vez, o valor do zinco foi escrito de
 * memória e estava errado; foi esta comparação que o apanhou.
 */
class VocabularioUnicoTest {

    private val vocabulario = File("../tools/catalogo/vocabulario.csv")
    private val referencias = File("src/commonMain/composeResources/files/seed_efsa_drv.csv")
    private val declaracao = File("src/commonMain/kotlin/pt/antares/app/core/nutrition/Nutrients.kt")

    private fun linhas(f: File): List<List<String>> =
        f.readLines().drop(1).filter { it.isNotBlank() }.map { it.split(",") }

    private fun chavesDoVocabulario(): Set<String> = linhas(vocabulario).map { it[0] }.toSet()

    private fun chavesDoCodigo(): Set<String> =
        Regex("""const val [A-Z_0-9]+ = "([^"]+)"""")
            .findAll(declaracao.readText())
            .map { it.groupValues[1] }
            .toSet()

    @Test
    fun `a app e o oleoduto conhecem as mesmas chaves`() {
        val noCodigo = chavesDoCodigo()
        val noVocabulario = chavesDoVocabulario()

        assertTrue(noCodigo.size > MINIMO_PLAUSIVEL, "só li ${noCodigo.size} chaves no código — a leitura partiu-se")

        assertEquals(
            emptySet(),
            noCodigo - noVocabulario,
            "estas chaves existem no código e não estão declaradas no vocabulário",
        )
        assertEquals(
            emptySet(),
            noVocabulario - noCodigo,
            "estas chaves estão declaradas no vocabulário e a app não as conhece",
        )
    }

    @Test
    fun `as referencias diarias dizem o mesmo nos dois ficheiros`() {
        val daEfsa = linhas(referencias).associate { it[0] to (it[1] to it[2]) }
        val doVocabulario = linhas(vocabulario)
            .filter { it[4].isNotBlank() }
            .associate { it[0] to (it[4] to it[5]) }

        assertEquals(
            daEfsa,
            doVocabulario,
            "o vocabulário e as referências que a app lê discordam. Manda o que a app lê.",
        )
    }

    @Test
    fun `cada chave diz a unidade em que esta`() {

        // A unidade não é decoração: `vitA_ug` e `vitD_ug` são microgramas, `vitE_mg` são
        // miligramas, e trocá-las por engano numa fonte nova dá um valor mil vezes maior sem
        // nada rebentar. O sufixo da chave e a coluna têm de concordar.
        val erradas = linhas(vocabulario)
            .filter { (it[0].substringAfterLast('_')) != it[2] }
            .map { "${it[0]} diz estar em ${it[2]}" }

        assertTrue(erradas.isEmpty(), "a unidade não bate com o sufixo da chave: $erradas")
    }

    @Test
    fun `um tagname por preencher diz porque`() {

        // Duas células estão vazias de propósito — são as que não se sabiam sem inventar. Uma
        // célula vazia é honesta; um tagname errado é uma armadilha para quem ligar a próxima
        // fonte. O que não se aceita é estar vazia sem se dizer que é de propósito.
        val semRazao = linhas(vocabulario)
            .filter { it[1].isBlank() && !it.drop(7).joinToString(",").contains("por confirmar") }
            .map { it[0] }

        assertTrue(semRazao.isEmpty(), "sem tagname e sem dizer porquê: $semRazao")
    }

    private companion object {
        const val MINIMO_PLAUSIVEL = 30
    }
}

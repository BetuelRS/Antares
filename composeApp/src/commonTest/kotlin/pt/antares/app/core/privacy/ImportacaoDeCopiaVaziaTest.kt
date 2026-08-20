package pt.antares.app.core.privacy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Substituir tudo por nada só pode destruir.
 *
 * A 2.1.0 pôs a app a escrever cópias sozinha na pasta de Documentos, e uma delas saiu
 * vazia — com os dois campos que a assinatura exige, portanto aceite como cópia legítima.
 * Escolher «substituir» com esse ficheiro esvaziava todas as tabelas restauráveis, catálogo
 * incluído, e o ficheiro tinha sido posto ali pela própria app.
 *
 * Não há caso nenhum em que trocar o que se tem por nada seja o que alguém quis.
 */
class ImportacaoDeCopiaVaziaTest {

    /**
     * Regista se a transação chegou a abrir. É o que dá valor a estes testes: recusar
     * **depois** de a transação abrir já teria apagado as tabelas.
     */
    private class BaseEspia : BackupDb {
        var abriu = false
        var truncou: List<String> = emptyList()

        override suspend fun emTransacao(aTruncar: List<String>, bloco: suspend () -> Unit) {
            abriu = true
            truncou = aTruncar
            bloco()
        }
    }

    private val vazia = """
        {
          "exportadoEm": "2026-08-20T20:15:09Z",
          "versaoApp": "2.1.0",
          "food_log": [],
          "weight_log": [],
          "foods": []
        }
    """.trimIndent()

    private val comUmaLinha = """
        {
          "exportadoEm": "2026-08-20T20:15:09Z",
          "versaoApp": "2.1.0",
          "food_log": [],
          "weight_log": [{"id": 1}]
        }
    """.trimIndent()

    private fun importador(base: BackupDb) =
        BackupImporter(sources = emptyList(), io = Dispatchers.Unconfined, db = base)

    @Test
    fun `substituir com uma copia sem linha nenhuma e recusado`() = runTest {
        val base = BaseEspia()
        val r = importador(base).import(vazia, ImportMode.REPLACE)

        assertIs<ImportResult.NotABackup>(r, "a cópia vazia passou")
        assertFalse(base.abriu, "a transação abriu: as tabelas já tinham sido esvaziadas")
    }

    @Test
    fun `a recusa diz porque`() = runTest {
        val r = importador(BaseEspia()).import(vazia, ImportMode.REPLACE)
        val recusa = assertIs<ImportResult.NotABackup>(r)
        assertTrue(recusa.porque.isNotBlank(), "recusou sem dizer nada que o ecrã possa mostrar")
    }

    @Test
    fun `juntar com uma copia vazia continua a ser deixado passar`() = runTest {

        // Juntar nada a alguma coisa não tira nada a ninguém. A guarda é só do caminho
        // destrutivo; recusar os dois seria proibir um gesto inofensivo.
        val base = BaseEspia()
        importador(base).import(vazia, ImportMode.MERGE)

        assertTrue(base.abriu, "juntar uma cópia vazia foi recusado como se fosse destrutivo")
        assertTrue(base.truncou.isEmpty(), "juntar chegou a esvaziar tabelas")
    }

    @Test
    fun `uma linha em qualquer tabela chega para substituir`() = runTest {

        // Não distingue tabelas de propósito: uma cópia com uma pesagem só continua a ser
        // uma cópia de alguém, e recusá-la seria a app a decidir o que é pouco.
        val base = BaseEspia()
        importador(base).import(comUmaLinha, ImportMode.REPLACE)

        assertTrue(base.abriu, "uma cópia com uma linha foi tratada como vazia")
    }

    @Test
    fun `um ficheiro sem a assinatura continua a ser recusado`() = runTest {
        val base = BaseEspia()
        val r = importador(base).import("""{"food_log": [{"id": 1}]}""", ImportMode.REPLACE)

        assertIs<ImportResult.NotABackup>(r)
        assertFalse(base.abriu)
    }
}

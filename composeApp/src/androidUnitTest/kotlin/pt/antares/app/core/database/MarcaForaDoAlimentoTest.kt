package pt.antares.app.core.database

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O que é da pessoa não volta para dentro da linha do alimento.
 *
 * Viveu lá até à v26, e custava duas coisas ao mesmo tempo. **O catálogo é substituído por
 * inteiro a cada versão** e a escrita grava a linha toda por cima — a 2.4.0 teve de as
 * transportar à mão para não as apagar, e um esquecimento nesse transporte apagava favoritos
 * sem dar erro. E **não iam na cópia de segurança**: dos alimentos só se exportam os que a
 * pessoa criou, portanto os favoritos do catálogo ficavam de fora e quem restaurasse uma
 * cópia perdia-os.
 *
 * Acrescentar outra vez uma coluna destas a `foods` traz os dois problemas de volta, e nem
 * um nem outro dá erro. Este teste lê o esquema exportado — o que a app vai mesmo criar no
 * telemóvel — e não o código: uma coluna pode nascer de uma anotação, de uma migração, ou de
 * um `@ColumnInfo` com outro nome, e é na base que ela aparece.
 */
class MarcaForaDoAlimentoTest {

    private val esquemas = File("schemas/pt.antares.app.core.database.AntaresDb")

    private val proibidas = setOf("isFavorite", "lastUsedAt", "lastAmountG")

    private fun tabelas(versao: Int): Map<String, List<String>> {
        val raiz = Json { ignoreUnknownKeys = true }
            .parseToJsonElement(File(esquemas, "$versao.json").readText())
            .jsonObject["database"]!!.jsonObject

        return raiz["entities"]!!.jsonArray.associate { elemento ->
            val entidade = elemento.jsonObject
            val nome = entidade["tableName"]!!.jsonPrimitive.content
            val colunas = entidade["fields"]?.jsonArray
                ?.map { it.jsonObject["columnName"]!!.jsonPrimitive.content }
                .orEmpty()
            nome to colunas
        }
    }

    private fun versaoMaisRecente(): Int =
        esquemas.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { it.nameWithoutExtension.toIntOrNull() }
            ?.maxOrNull()
            ?: error("não encontrei esquemas exportados em ${esquemas.path}")

    @Test
    fun `a tabela de alimentos nao tem nada da pessoa`() {
        val versao = versaoMaisRecente()
        val colunas = tabelas(versao)["foods"] ?: error("a tabela `foods` desapareceu do esquema v$versao")

        val voltaram = colunas.filter { it in proibidas }
        assertTrue(
            voltaram.isEmpty(),
            "estas colunas voltaram para dentro de `foods` na v$versao: $voltaram. " +
                "O que é da pessoa vive na `food_marca` — na linha do alimento, é apagado " +
                "pela actualização do catálogo e não vai na cópia de segurança.",
        )
    }

    @Test
    fun `a tabela de marcas existe e tem as tres`() {

        // O contrário do teste de cima, e é preciso: apagar as colunas de `foods` sem haver
        // para onde as mandar passaria no primeiro teste com nota máxima.
        val versao = versaoMaisRecente()
        val colunas = tabelas(versao)["food_marca"]
            ?: error("a tabela `food_marca` não existe no esquema v$versao")

        assertEquals(
            proibidas,
            colunas.filter { it in proibidas }.toSet(),
            "a `food_marca` deixou de guardar uma das três coisas que são da pessoa",
        )
        assertTrue("foodId" in colunas, "a `food_marca` perdeu a ligação ao alimento")
    }
}

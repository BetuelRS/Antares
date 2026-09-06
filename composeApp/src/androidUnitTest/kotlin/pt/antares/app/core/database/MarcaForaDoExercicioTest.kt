package pt.antares.app.core.database

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * O gémeo do [MarcaForaDoAlimentoTest], para os exercícios.
 *
 * O catálogo de exercícios ainda **não** é substituído por inteiro como o dos alimentos — o
 * `ExerciseSeeder` semeia uma vez e depois só corrige nomes —, e por isso a tentação de pôr
 * o favorito numa coluna da `exercise` parece inofensiva. Não é, por duas razões que já se
 * pagaram uma vez do outro lado:
 *
 * **A exportação.** Dos exercícios só se exportam os que a pessoa criou — o catálogo não se
 * exporta, por ser grande e reconstruível. Um favorito dentro da linha do catálogo fica de
 * fora da cópia de segurança, e quem restaurar perde meses de escolhas sem aviso nenhum.
 *
 * **E o dia em que o catálogo passar a ser substituído.** A 2.4.0 teve de transportar as
 * colunas do utilizador à mão para a escrita do catálogo não as apagar, e a 2.5.0 tirou-as
 * de lá para o problema deixar de poder existir. Repetir a coluna aqui é pôr esse dia a
 * depender de alguém se lembrar.
 *
 * Lê o esquema exportado, e não o código: a coluna pode nascer de uma anotação ou de uma
 * migração, e é na base que ela aparece.
 */
class MarcaForaDoExercicioTest {

    private val esquemas = File("schemas/pt.antares.app.core.database.AntaresDb")

    private val proibidas = setOf("isFavorite", "lastUsedAt")

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
    fun `a tabela de exercicios nao tem nada da pessoa`() {
        val versao = versaoMaisRecente()
        val colunas = tabelas(versao)["exercise"]
            ?: error("a tabela `exercise` desapareceu do esquema v$versao")

        val voltaram = colunas.filter { it in proibidas }
        assertTrue(
            voltaram.isEmpty(),
            "estas colunas entraram na `exercise` na v$versao: $voltaram. O que é da pessoa " +
                "vive na `exercise_marca` — na linha do catálogo, não vai na cópia de segurança.",
        )
    }

    @Test
    fun `a tabela de marcas existe e liga ao exercicio`() {

        // O contrário do teste de cima, e é preciso pela mesma razão: não haver coluna
        // nenhuma em lado nenhum passaria no primeiro com nota máxima.
        val versao = versaoMaisRecente()
        val colunas = tabelas(versao)["exercise_marca"]
            ?: error("a tabela `exercise_marca` não existe no esquema v$versao")

        assertTrue("exerciseId" in colunas, "a `exercise_marca` perdeu a ligação ao exercício")

        // E não ganhou uma coluna a repetir o que a existência da linha já diz.
        assertTrue(
            colunas.none { it in proibidas },
            "a `exercise_marca` ganhou ${colunas.filter { it in proibidas }}: a linha é o facto",
        )
    }
}

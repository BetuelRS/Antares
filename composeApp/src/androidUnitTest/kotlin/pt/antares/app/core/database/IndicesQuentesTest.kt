package pt.antares.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Os índices de que as consultas mais repetidas dependem.
 *
 * Um índice em falta não parte nada: a consulta devolve o mesmo, só que a varrer a tabela
 * toda. Com 11 432 séries isso não se nota num telemóvel a estrear e nota-se a sério ao fim
 * de um ano — e a causa é invisível a quem lê o código, porque a linha de SQL não muda.
 *
 * A lista abaixo é o que **tem** de existir, e cada linha diz o que varreria sem ele. Não é
 * a lista de todos os índices da base: é a dos que, se alguém apagar a pensar que sobram,
 * levam a app abaixo aos poucos.
 */
@RunWith(RobolectricTestRunner::class)
class IndicesQuentesTest {

    private val db: AntaresDb = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AntaresDb::class.java,
    ).setQueryCoroutineContext(Dispatchers.Default).build()

    @AfterTest
    fun tearDown() = db.close()

    private val obrigatorios = mapOf(
        "workout_set" to setOf(
            // As séries da sessão, as séries-fantasma e o histórico do exercício.
            "sessionId",
            // Os recordes, o 1RM e o progresso por exercício.
            "exerciseId",
        ),
        // Os pontos de GPS de uma corrida, lidos para desenhar o percurso.
        "track_point" to setOf("runId"),
        // O dia inteiro do diário, e cada refeição dentro dele.
        "food_log" to setOf("epochDay", "foodId"),
        // A tabela cresce com o histórico todo e lê-se sempre por dia.
        "exercise_log" to setOf("epochDay"),
        "routine_item" to setOf("routineId", "exerciseId"),
        "recipe_ingredient" to setOf("recipeId"),
        "meal_template_item" to setOf("templateId"),
    )

    private fun primeirasColunasIndexadas(tabela: String): Set<String> {
        val indices = mutableListOf<String>()
        db.openHelper.readableDatabase.query("PRAGMA index_list(`$tabela`)").use { c ->
            val nome = c.getColumnIndexOrThrow("name")
            while (c.moveToNext()) indices += c.getString(nome)
        }

        // Só a primeira coluna de cada índice conta: um índice composto serve consultas
        // pelo prefixo, e não por uma coluna do meio.
        return indices.mapNotNull { indice ->
            db.openHelper.readableDatabase.query("PRAGMA index_info(`$indice`)").use { c ->
                if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow("name")) else null
            }
        }.toSet()
    }

    @Test
    fun `as consultas mais repetidas tem indice por onde entrar`() {
        val emFalta = obrigatorios.mapNotNull { (tabela, colunas) ->
            val existentes = primeirasColunasIndexadas(tabela)
            (colunas - existentes).takeIf { it.isNotEmpty() }?.let { "$tabela: $it" }
        }

        assertEquals(
            emptyList(),
            emFalta,
            "estas colunas deixaram de ter índice. A app continua a dar as respostas certas " +
                "e passa a varrer a tabela inteira para as dar — o custo cresce com os anos " +
                "de uso de quem já lá tem dados, e não aparece em teste nenhum.",
        )
    }
}

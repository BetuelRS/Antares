package pt.antares.app.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "recipe")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    // Peso depois de cozinhar. Nulo usa a soma dos ingredientes — ver [RecipeCalc.compute],
    // onde a água evaporada concentra a nutrição por 100 g.
    val yieldGrams: Double?,
    // Quantas doses a receita dá. Nulo é o que havia antes: regista-se em gramas, e quem
    // fez uma lasanha tinha de saber quantos gramas comeu dela.
    val servings: Int? = null,
    // Como se cozinhou o prato — o `id` de um método da tabela de confeção. Nulo é o que
    // havia antes, e continua a ser o que fica em receitas que não vão ao lume: nenhuma
    // retenção se aplica, e a receita é a soma dos ingredientes como sempre foi.
    val metodo: String? = null,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Serializable
@Entity(
    tableName = "recipe_ingredient",
    indices = [Index("recipeId"), Index("foodId")],
)
/**
 * Um ingrediente aponta para o alimento em vez de lhe copiar a nutrição: ao contrário do
 * diário, uma receita deve refletir a correção feita no alimento.
 */
data class RecipeIngredientEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val foodId: String,
    val grams: Double,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Serializable
@Entity(
    tableName = "recipe_step",
    indices = [Index("recipeId")],
)
/**
 * Um passo de preparação.
 *
 * **É a única coisa numa receita que a app não sabe conferir.** Os ingredientes somam-se, o
 * peso final compara-se com as tabelas, a nutrição recalcula-se — um passo é texto que quem
 * cozinhou escreveu para si próprio, e não entra em conta nenhuma. É de propósito: uma
 * receita continua a ser um alimento composto, e os passos são o que faltava para ela também
 * servir para cozinhar.
 *
 * Ao contrário do ingrediente, **a ordem é o dado**: «leva ao lume» antes de «tempera» é
 * outra receita. Por isso há [posicao] e não a ordem de introdução — reordenar ingredientes
 * não muda nada, reordenar passos muda tudo.
 */
data class RecipeStepEntity(
    @PrimaryKey val id: String,
    val recipeId: String,

    /**
     * A posição na lista, a contar de zero e sem buracos.
     *
     * Renumera-se a lista inteira a cada movimento, em vez de se guardarem posições
     * fracionárias entre vizinhos: uma receita tem passos que se contam pelos dedos, e a
     * aritmética de chaves fracionárias custava mais a ler do que o que poupava.
     */
    val posicao: Int,
    val texto: String,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

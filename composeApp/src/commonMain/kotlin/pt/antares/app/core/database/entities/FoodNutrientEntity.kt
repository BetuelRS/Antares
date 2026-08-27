package pt.antares.app.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable

/**
 * Um micronutriente de um alimento, numa linha própria.
 *
 * Os micros continuam a viver no `microsJson` do `foods`, que é o que o resto da app lê ao
 * mostrar uma ficha. Esta tabela é a mesma informação virada ao contrário, para responder à
 * pergunta oposta: *que alimentos têm este nutriente?*
 *
 * O JSON não sabe responder a isso. Procurá-lo com `LIKE` obrigava a trazer milhares de
 * linhas inteiras para memória e a descodificar JSON uma a uma, a cada toque num nutriente.
 * Aqui a mesma pergunta é um índice.
 *
 * **A tabela é derivada, e por isso é descartável.** É reconstruída a partir do JSON pelo
 * `FoodSeeder`; perder as linhas custa uma sementeira, não custa dados.
 */
@Serializable
@Entity(
    tableName = "food_nutrient",
    primaryKeys = ["foodId", "key"],
    indices = [Index("key")],
    
)
data class FoodNutrientEntity(
    val foodId: String,

    // A chave canónica do nutriente — as mesmas do `Nutrients`, e as mesmas do lado Deno.
    val key: String,

    // Por 100 g do alimento, como no JSON de onde veio. Escalar é de quem lê.
    val value: Double,
)

package pt.antares.app.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * O que é da pessoa sobre um alimento: se o marcou, quando o usou, e quanto usou.
 *
 * Até à v26 estas três colunas viviam dentro da linha do alimento, e isso tinha dois preços
 * que só se veem quando se olha para eles ao mesmo tempo.
 *
 * O primeiro é que **o catálogo é substituído por inteiro a cada versão**, e a escrita grava
 * a linha toda por cima. A 2.4.0 corrigiu isso à mão, fazendo as colunas viajarem dentro da
 * linha nova. Aqui o problema deixa de poder existir: o que não vive na linha do alimento não
 * pode ser apagado ao escrevê-la.
 *
 * O segundo é que **não iam na cópia de segurança**. A exportação de alimentos só leva os que
 * a pessoa criou — o catálogo não se exporta, por ser grande e reconstruível — e por isso os
 * favoritos e os recentes, que estavam nas linhas do catálogo, ficavam de fora. Quem
 * restaurasse uma cópia perdia-os, sem aviso nenhum. Numa tabela própria são uma tabela como
 * as outras, e exportam-se como as outras.
 *
 * Não tem chave estrangeira para `foods` de propósito: na 2.5.1 o catálogo passa para uma
 * base própria, e uma chave estrangeira entre bases não existe. A regra de integridade é
 * outra, e está no [pt.antares.app.feature.fooddata.FoodRepository]: uma marca sem alimento
 * é ruído, não é erro — e apagá-la seria apagar a intenção de quem a deixou.
 */
@Serializable
@Entity(
    tableName = "food_marca",
    indices = [Index("isFavorite"), Index("lastUsedAt")],
)
data class FoodMarkEntity(
    @PrimaryKey val foodId: String,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long = 0L,

    // A última quantidade serve de sugestão quando ainda não há registos que cheguem para
    // a porção habitual do [pt.antares.app.core.calc.UsualPortion].
    val lastAmountG: Double? = null,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

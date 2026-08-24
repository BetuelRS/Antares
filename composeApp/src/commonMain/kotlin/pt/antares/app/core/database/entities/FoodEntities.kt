package pt.antares.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot

/**
 * Um alimento do catálogo. Os índices servem os três modos de o encontrar sem escrever
 * nada: recentes, favoritos e por origem.
 *
 * Nota sobre a coluna que aparece em quase todas as tabelas: `deleted` marca lápides — as
 * linhas apagadas ficam, e por isso praticamente todas as consultas filtram `deleted = 0`.
 */
@Serializable
@Entity(
    tableName = "foods",

    // O que a pessoa marcou saiu daqui na v27 e vive no [FoodMarkEntity]. Sobra o índice
    // por origem, que é o que separa o catálogo do que ela própria criou.
    indices = [Index("source")],
)
data class FoodEntity(
    @PrimaryKey val id: String,
    val source: FoodSource,

    // Identificador na origem — o código de barras, no caso da Open Food Facts. Permite
    // reconhecer o mesmo alimento sem depender do nome.
    val sourceRef: String?,
    // Os dois nomes vivem lado a lado em vez de haver uma tabela de traduções: o catálogo
    // é fixo e de dois idiomas, e ambos entram no índice de pesquisa.
    val namePt: String,
    val nameEn: String,
    val brand: String?,
    val kcal: Int,
    val proteinG: Double,
    val carbsG: Double,
    val sugarsG: Double?,
    val fatG: Double,
    val satFatG: Double?,

    // Micronutrientes por 100 g, em JSON: são umas dezenas, quase sempre ausentes, e uma
    // coluna por cada deixaria a tabela quase toda a null. Nulo aqui é o que faz um
    // alimento não contribuir para os ecrãs de micronutrientes.
    val microsJson: String?,
    val servingName: String?,
    val servingGrams: Double?,

    // Separa mililitros de gramas na apresentação; a nutrição continua toda por 100 g.
    @ColumnInfo(defaultValue = "0") val isLiquid: Boolean = false,

    val verified: Boolean = false,

    /**
     * A família de confeção — `legumes`, `vaca`, `peixe` — ou nulo.
     *
     * É por ela que se sabe o que acontece a este alimento quando se cozinha: o USDA publica
     * a retenção de nutrientes e o rendimento de peso **por grupo de alimento**, e não por
     * alimento, porque o que sobrevive a ferver é uma propriedade do que se está a ferver.
     *
     * **Nulo quer dizer «não se cozinha isto», e não «não sabemos».** Um pão já foi ao forno,
     * um gelado não vai, um prato composto é comida feita. É o que faz a app não oferecer
     * «e se for cozido?» a metade do catálogo sem ter de explicar porquê.
     */
    val familia: String? = null,

    val updatedAt: Long,
    val deleted: Boolean = false,
)

/**
 * Índice de pesquisa em texto livre. Tabela à parte porque o FTS4 do SQLite guarda tudo
 * como texto e não sabe de tipos — daí a ligação por [foodId] à tabela verdadeira.
 */
@Fts4(notIndexed = ["foodId"])
@Entity(tableName = "foods_fts")
data class FoodFtsEntity(
    val foodId: String,

    // Nomes e marca já concatenados e normalizados. Guardar o texto pronto poupa fazer a
    // mesma normalização a cada tecla escrita na pesquisa.
    val searchText: String,
)

@Serializable
@Entity(
    tableName = "food_log",
    indices = [Index(value = ["epochDay", "mealSlot"]), Index("foodId")],
)
/**
 * Uma refeição registada. Tudo o que interessa é copiado para aqui no momento do
 * registo — os campos `Snapshot` — para o diário de ontem não mudar quando alguém
 * corrigir o alimento hoje. Por isso [foodId] é anulável: o registo sobrevive ao
 * alimento ser apagado.
 */
data class FoodLogEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val mealSlot: MealSlot,
    val foodId: String?,
    val nameSnapshot: String,
    val quantityGrams: Double,
    // Já multiplicados pela quantidade: são o que este registo vale, não o que valem
    // 100 g. Somar os dias é somar estas colunas.
    val kcalSnapshot: Int,
    val proteinSnapshot: Double,
    val carbsSnapshot: Double,
    val fatSnapshot: Double,

    // Ao contrário dos macros, os micronutrientes ficam por 100 g e escalam na leitura.
    // Nulo aqui apaga o registo dos ecrãs de micronutrientes sem o apagar do diário.
    val microsPer100Json: String?,
    val origin: LogOrigin = LogOrigin.MANUAL,

    @ColumnInfo(defaultValue = "0") val isLiquid: Boolean = false,

    /**
     * A que horas se comeu, em minutos desde a meia-noite local — de 0 a 1439.
     *
     * Não se confunde com o [updatedAt], que é quando a linha foi escrita: registar o
     * jantar na manhã seguinte dá dois valores muito diferentes, e só este serve para
     * saber quando a pessoa come. É dele que sai a janela alimentar e o cruzamento com o
     * jejum.
     *
     * **Nulo é um valor legítimo, e não um valor em falta por preencher.** Fica nulo em
     * todo o histórico anterior a esta coluna, e em qualquer registo feito num dia que não
     * seja o de hoje: aí a app não sabe a que horas se comeu, e inventar uma hora estraga
     * as contas que dependem dela.
     */
    @ColumnInfo(defaultValue = "NULL") val eatenAtMin: Int? = null,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

/**
 * A água do dia numa linha só, e daí o índice único: o total substitui-se, não se
 * acumula em registos.
 *
 * O único cuidado é o índice não distinguir lápides. Uma linha apagada continua a ocupar
 * o dia, e inserir outra para a mesma data falha — quem inserir em massa tem de limpar as
 * lápides primeiro, como o [DemoDao] faz.
 */
@Serializable
@Entity(
    tableName = "water_log",
    indices = [Index(value = ["epochDay"], unique = true)],
)
data class WaterLogEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val ml: Int,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

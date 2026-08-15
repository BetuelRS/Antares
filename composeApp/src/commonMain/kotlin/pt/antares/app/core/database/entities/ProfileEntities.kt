package pt.antares.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.calc.BmrFormula
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.EnergyUnit
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.WeightSource
import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.model.Sex
import pt.antares.app.core.model.UnitSystem

/**
 * O perfil. Tabela de uma linha só — a chave é fixa — para o perfil se ler e escrever
 * sem primeiro descobrir qual é.
 *
 * O peso não está aqui: vive no `weight_log`, e as contas usam a pesagem mais recente.
 * Duplicá-lo daria duas verdades sobre o mesmo número.
 */
@Serializable
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = SINGLETON_ID,
    val sex: Sex,
    val birthEpochDay: Long,
    val heightCm: Int,
    val activityLevel: ActivityLevel,
    val goalType: GoalType,

    // O ritmo guarda-se em kcal por dia e não em kg por semana: é a unidade em que entra
    // na conta, e converter na leitura fazia o alvo dançar com o arredondamento.
    // Negativo é perder, positivo é ganhar.
    val goalRateKcal: Int,
    val macroStrategy: MacroStrategy,
    // Só valem com a estratégia manual; nas outras são ignorados em vez de apagados, para
    // quem alternar não perder o que tinha configurado.
    val customProteinG: Int?,
    val customCarbsG: Int?,
    val customFatG: Int?,

    val exerciseAddBack: Boolean = true,

    @ColumnInfo(defaultValue = "NULL") val goalWeightKg: Double? = null,

    // A massa gorda mais recente, copiada para aqui para o basal não ter de consultar o
    // histórico a cada cálculo. O `body_measurement_log` guarda a série toda.
    @ColumnInfo(defaultValue = "NULL") val bodyFatPct: Double? = null,
    // A origem decide se o valor serve para a Katch-McArdle — ver `usableLeanMassKg`.
    @ColumnInfo(defaultValue = "NULL") val bodyFatSource: BodyFatSource? = null,

    @ColumnInfo(defaultValue = "NULL") val waistCm: Double? = null,
    @ColumnInfo(defaultValue = "NULL") val neckCm: Double? = null,
    @ColumnInfo(defaultValue = "NULL") val hipCm: Double? = null,

    // Só escolhe entre as duas fórmulas de massa magra; sem massa gorda utilizável a app
    // usa a Mifflin de qualquer maneira.
    @ColumnInfo(defaultValue = "NULL") val bmrFormulaOverride: BmrFormula? = null,

    // Objetivo alternativo ao peso, para quem treina: perder gordura sem perder peso não
    // aparece na balança.
    @ColumnInfo(defaultValue = "NULL") val goalBodyFatPct: Double? = null,

    @ColumnInfo(defaultValue = "NULL") val heightConfirmedEpochDay: Long? = null,

    // Janela da tendência de peso escolhida no perfil; a null usa-se a de omissão do
    // `weeklyRateKg`.
    @ColumnInfo(defaultValue = "NULL") val trendWindowDays: Int? = null,

    // Gravidez e amamentação anulam o défice — ver `removesDeficit`.
    @ColumnInfo(defaultValue = "NULL") val lifeStage: LifeStage? = null,

    // Preferências de apresentação. Toda a base guarda métrico e kcal; a conversão é só
    // no ecrã, para os dados não dependerem da preferência do momento.
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val energyUnit: EnergyUnit = EnergyUnit.KCAL,
    val updatedAt: Long,
    val deleted: Boolean = false,
) {
    companion object {
        const val SINGLETON_ID = "profile"
    }
}

/**
 * Uma pesagem por dia — daí o índice único. Pesar-se duas vezes substitui; guardar as
 * duas obrigaria a escolher uma na leitura, e a tendência do [WeightTrend] parte de um
 * ponto por dia.
 *
 * As lápides ocupam o dia tal como as linhas vivas: quem inserir em massa tem de as
 * limpar primeiro.
 */
@Serializable
@Entity(
    tableName = "weight_log",
    indices = [Index(value = ["epochDay"], unique = true)],
)
data class WeightLogEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val weightKg: Double,
    val note: String?,

    // Manual ou vinda da balança ligada ao telemóvel. Distingue-as no gráfico e evita
    // reimportar o que já cá está.
    @ColumnInfo(defaultValue = "MANUAL")
    val source: WeightSource = WeightSource.MANUAL,

    val sourceRef: String? = null,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Serializable
@Entity(
    tableName = "daily_target_override",
    indices = [Index(value = ["epochDay"], unique = true)],
)
/**
 * Metas fixadas para um dia concreto, que se sobrepõem ao cálculo. É o que faz a proposta
 * adaptativa, uma vez aceite, valer para a frente sem reescrever o perfil.
 */
data class DailyTargetOverrideEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,

    // Quem fixou o dia. Texto livre para o ecrã poder explicar a origem sem a app ter de
    // conhecer de antemão todas as que virão.
    val source: String,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Serializable
@Entity(
    tableName = "body_measurement_log",
    indices = [Index(value = ["epochDay"], unique = true)],
)
data class BodyMeasurementEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val bodyFatPct: Double? = null,

    val bodyFatSource: BodyFatSource? = null,
    val waistCm: Double? = null,
    val neckCm: Double? = null,
    val hipCm: Double? = null,

    @ColumnInfo(defaultValue = "NULL") val armCm: Double? = null,
    @ColumnInfo(defaultValue = "NULL") val thighCm: Double? = null,
    @ColumnInfo(defaultValue = "NULL") val chestCm: Double? = null,
    val updatedAt: Long,
    val deleted: Boolean = false,
) {

    // Todas as medidas são opcionais, por isso é possível gravar uma linha que não diz
    // nada. O ecrã usa isto para recusar em vez de encher o histórico de dias vazios.
    val isEmpty: Boolean
        get() = bodyFatPct == null && waistCm == null && neckCm == null && hipCm == null &&
            armCm == null && thighCm == null && chestCm == null
}

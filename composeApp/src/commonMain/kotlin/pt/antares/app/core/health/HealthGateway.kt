package pt.antares.app.core.health

/**
 * A fronteira com o Health Connect. Existe como interface para o código comum poder falar
 * de saúde sem depender do Android, e para os testes correrem sem o serviço instalado.
 *
 * Leituras e escritas têm permissões separadas de propósito: a app pede uma de cada vez,
 * quando e se a pessoa quiser a funcionalidade que a exige.
 */
interface HealthGateway {

    fun availability(): HealthAvailability

    val readPermissions: Set<String>

    suspend fun hasReadPermissions(): Boolean

    suspend fun steps(startMs: Long, endMs: Long): Long?

    suspend fun weights(sinceMs: Long): List<HealthWeight>

    suspend fun bodyComposition(sinceMs: Long): List<HealthBodyComposition>

    suspend fun sessions(sinceMs: Long): List<HealthSession>

    val writePermissions: Set<String>

    suspend fun hasWritePermissions(): Boolean

    suspend fun writeNutrition(
        epochDay: Long,
        kcal: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,

        micros: Map<String, Double> = emptyMap(),
    )

    suspend fun writeSession(session: OutboundSession): Boolean

    suspend fun writeBodyComposition(
        epochDay: Long,
        bodyFatPct: Double?,
        leanMassKg: Double?,
    ): Boolean
}

data class OutboundSession(

    // Identificador do lado da app, para reescrever a mesma sessão em vez de a duplicar
    // quando um treino é editado depois de já ter sido publicado.
    val clientId: String,
    val kind: OutboundKind,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val kcal: Int,
)

enum class OutboundKind { WORKOUT, RUN }

/**
 * A implementação que não faz nada, usada quando o Health Connect não existe no aparelho.
 * Devolve vazio e falso em vez de lançar: o resto da app não tem de perguntar se há
 * serviço antes de cada chamada.
 */
object NoHealthGateway : HealthGateway {
    override fun availability() = HealthAvailability.NOT_SUPPORTED
    override val readPermissions: Set<String> = emptySet()
    override suspend fun hasReadPermissions() = false
    override suspend fun steps(startMs: Long, endMs: Long): Long? = null
    override suspend fun weights(sinceMs: Long): List<HealthWeight> = emptyList()
    override suspend fun bodyComposition(sinceMs: Long): List<HealthBodyComposition> = emptyList()
    override suspend fun sessions(sinceMs: Long): List<HealthSession> = emptyList()
    override val writePermissions: Set<String> = emptySet()
    override suspend fun hasWritePermissions() = false
    override suspend fun writeNutrition(
        epochDay: Long,
        kcal: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        micros: Map<String, Double>,
    ) = Unit
    override suspend fun writeSession(session: OutboundSession) = false
    override suspend fun writeBodyComposition(epochDay: Long, bodyFatPct: Double?, leanMassKg: Double?) = false
}

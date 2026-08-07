package pt.antares.app.core.health

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

    val clientId: String,
    val kind: OutboundKind,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val kcal: Int,
)

enum class OutboundKind { WORKOUT, RUN }

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

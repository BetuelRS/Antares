package pt.antares.app.testing

import pt.antares.app.core.health.HealthAvailability
import pt.antares.app.core.health.HealthBodyComposition
import pt.antares.app.core.health.HealthGateway
import pt.antares.app.core.health.HealthSession
import pt.antares.app.core.health.HealthWeight
import pt.antares.app.core.health.OutboundSession

class CountingHealthGateway : HealthGateway {

    var stepsCalls = 0
        private set
    var weightsCalls = 0
        private set
    var sessionsCalls = 0
        private set
    var nutritionWrites = 0
        private set
    var sessionWrites = 0
        private set

    val totalCalls: Int
        get() = stepsCalls + weightsCalls + sessionsCalls + nutritionWrites + sessionWrites

    override fun availability() = HealthAvailability.AVAILABLE

    override val readPermissions: Set<String> = setOf("r")
    override suspend fun hasReadPermissions() = true

    override suspend fun steps(startMs: Long, endMs: Long): Long? {
        stepsCalls++
        return null
    }

    override suspend fun weights(sinceMs: Long): List<HealthWeight> {
        weightsCalls++
        return emptyList()
    }

    override suspend fun bodyComposition(sinceMs: Long): List<HealthBodyComposition> = emptyList()

    override suspend fun sessions(sinceMs: Long): List<HealthSession> {
        sessionsCalls++
        return emptyList()
    }

    override val writePermissions: Set<String> = setOf("w")
    override suspend fun hasWritePermissions() = true

    override suspend fun writeNutrition(
        epochDay: Long,
        kcal: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        micros: Map<String, Double>,
    ) {
        nutritionWrites++
    }

    override suspend fun writeSession(session: OutboundSession): Boolean {
        sessionWrites++
        return true
    }

    override suspend fun writeBodyComposition(
        epochDay: Long,
        bodyFatPct: Double?,
        leanMassKg: Double?,
    ): Boolean = true
}

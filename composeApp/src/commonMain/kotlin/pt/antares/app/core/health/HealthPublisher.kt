package pt.antares.app.core.health

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

data class DayNutrition(
    val epochDay: Long,
    val kcal: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,

    val micros: Map<String, Double> = emptyMap(),
)

data class OutboundBodyComposition(
    val epochDay: Long,
    val bodyFatPct: Double?,
    val leanMassKg: Double?,
)

data class HealthPublish(
    val nutritionDays: Int = 0,
    val sessions: Int = 0,
    val bodyMeasurements: Int = 0,
) {
    val isEmpty: Boolean get() = nutritionDays == 0 && sessions == 0 && bodyMeasurements == 0
}

/**
 * O sentido inverso do [HealthRepository]: escreve no Health Connect o que a app sabe, para
 * outras apps o poderem ler.
 *
 * Isto **não** é uma saída para fora do telemóvel: o Health Connect é do sistema, e a troca
 * acontece entre apps do mesmo aparelho, com autorização dada nas definições. O que sai mesmo
 * do telemóvel são a pesquisa na Open Food Facts, a análise por foto e por texto, os quadrados
 * do mapa nas corridas e as imagens dos exercícios.
 */
class HealthPublisher(
    private val gateway: HealthGateway,
    private val nutrition: NutritionSource,
    private val sessions: SessionSource,
    private val bodyComposition: BodyCompositionSource,
    private val lastPublishAt: suspend () -> Long,
    private val setLastPublishAt: suspend (Long) -> Unit,
    private val io: CoroutineDispatcher,
    private val now: () -> Long,
) {

    fun interface NutritionSource {
        suspend fun since(fromEpochDay: Long): List<DayNutrition>
    }

    fun interface SessionSource {
        suspend fun endedSince(fromMs: Long): List<OutboundSession>
    }

    fun interface BodyCompositionSource {
        suspend fun since(fromEpochDay: Long): List<OutboundBodyComposition>
    }

    val writePermissions: Set<String> get() = gateway.writePermissions

    suspend fun hasPermissions(): Boolean = gateway.hasWritePermissions()

    suspend fun publishNow(epochDayToday: Long): HealthPublish = withContext(io) {
        if (gateway.availability() != HealthAvailability.AVAILABLE) return@withContext HealthPublish()
        if (!gateway.hasWritePermissions()) return@withContext HealthPublish()

        val startedAt = now()
        val since = lastPublishAt()

        val fromDay = epochDayToday - NUTRITION_LOOKBACK_DAYS
        var nutritionDays = 0
        for (day in nutrition.since(fromDay)) {
            gateway.writeNutrition(day.epochDay, day.kcal, day.proteinG, day.carbsG, day.fatG, day.micros)
            nutritionDays++
        }

        val fromMs = (since - SESSION_SLACK_MS).coerceAtLeast(0L)
        var published = 0
        for (s in sessions.endedSince(fromMs)) {
            if (gateway.writeSession(s)) published++
        }

        var bodyPublished = 0
        for (m in bodyComposition.since(fromDay)) {
            if (gateway.writeBodyComposition(m.epochDay, m.bodyFatPct, m.leanMassKg)) bodyPublished++
        }

        setLastPublishAt(startedAt)
        HealthPublish(
            nutritionDays = nutritionDays,
            sessions = published,
            bodyMeasurements = bodyPublished,
        )
    }

    companion object {

        // A nutrição reescreve sempre os últimos dias em vez de seguir a marca de água:
        // um dia já publicado continua a ser editado, e só reescrevendo é que o valor lá
        // fora acompanha as correções.
        const val NUTRITION_LOOKBACK_DAYS = 2L

        // Um dia de folga para trás nas sessões: um treino fechado pouco antes da última
        // publicação ficaria de fora se a janela começasse exatamente nela.
        const val SESSION_SLACK_MS = 24L * 60 * 60 * 1000
    }
}

package pt.antares.app.core.health

enum class HealthAvailability {

    AVAILABLE,

    PROVIDER_UPDATE_REQUIRED,

    NOT_SUPPORTED,
}

data class HealthWeight(

    val uid: String,
    val timestampMs: Long,
    val kg: Double,
)

data class HealthBodyComposition(
    val uid: String,
    val timestampMs: Long,
    val bodyFatPct: Double? = null,
    val leanMassKg: Double? = null,
)

data class HealthSession(
    val uid: String,

    val title: String?,

    val activity: String,
    val startMs: Long,
    val endMs: Long,

    val kcal: Int?,

    val met: Double?,
)

data class TimeWindow(val startMs: Long, val endMs: Long)

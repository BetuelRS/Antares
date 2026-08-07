package pt.antares.app.feature.achievements

enum class AchievementCategory { WORKOUTS, RUN_KM, FASTS, WEIGHINS, WAIST_CM, BODY_FAT_PCT }

data class Achievement(
    val category: AchievementCategory,
    val target: Int,
    val current: Int,
) {
    val unlocked: Boolean get() = current >= target

    val fraction: Float get() = if (target <= 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)
}

data class AchievementStats(
    val workouts: Int = 0,
    val runKm: Int = 0,
    val fastsCompleted: Int = 0,
    val weighIns: Int = 0,

    val waistCmLost: Int = 0,

    val bodyFatPctLost: Int = 0,
)

object AchievementCalc {

    private val targets: Map<AchievementCategory, List<Int>> = mapOf(
        AchievementCategory.WORKOUTS to listOf(1, 10, 25, 50, 100),
        AchievementCategory.RUN_KM to listOf(5, 10, 50, 100, 250),
        AchievementCategory.FASTS to listOf(1, 10, 30),
        AchievementCategory.WEIGHINS to listOf(1, 10, 30),

        AchievementCategory.WAIST_CM to listOf(2, 5, 10),
        AchievementCategory.BODY_FAT_PCT to listOf(1, 3, 5),
    )

    fun build(stats: AchievementStats): List<Achievement> {
        fun rows(cat: AchievementCategory, current: Int) =
            targets.getValue(cat).map { Achievement(cat, it, current) }
        return rows(AchievementCategory.WORKOUTS, stats.workouts) +
            rows(AchievementCategory.RUN_KM, stats.runKm) +
            rows(AchievementCategory.FASTS, stats.fastsCompleted) +
            rows(AchievementCategory.WEIGHINS, stats.weighIns) +
            rows(AchievementCategory.WAIST_CM, stats.waistCmLost) +
            rows(AchievementCategory.BODY_FAT_PCT, stats.bodyFatPctLost)
    }

    fun bestDrop(values: List<Double>): Int {
        if (values.size < 2) return 0
        val primeiro = values.first()
        val melhor = values.min()
        return (primeiro - melhor).toInt().coerceAtLeast(0)
    }

    fun unlockedCount(stats: AchievementStats): Int = build(stats).count { it.unlocked }

    fun total(): Int = targets.values.sumOf { it.size }
}

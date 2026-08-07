package pt.antares.app.feature.running.domain

object RunPrCalc {

    fun bestTimeMs(activities: List<List<Split>>, km: Int): Long? =
        activities.mapNotNull { timeForKm(it, km) }.minOrNull()

    fun timeForKm(splits: List<Split>, km: Int): Long? {

        val full = splits.filter { it.distanceM >= 999.0 }
        if (full.size < km) return null
        return full.take(km).sumOf { it.movingMs }
    }
}

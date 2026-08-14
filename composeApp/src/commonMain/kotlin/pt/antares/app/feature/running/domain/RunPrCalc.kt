package pt.antares.app.feature.running.domain

/**
 * Recordes de corrida por distância. O tempo dos primeiros N quilómetros, e não o melhor
 * troço da corrida: é uma aproximação, mas comparável entre atividades.
 */
object RunPrCalc {

    fun bestTimeMs(activities: List<List<Split>>, km: Int): Long? =
        activities.mapNotNull { timeForKm(it, km) }.minOrNull()

    fun timeForKm(splits: List<Split>, km: Int): Long? {

        // O último parcial de uma corrida está quase sempre incompleto — a corrida acabou
        // a meio do quilómetro — e somá-lo dava um tempo bom por uma distância menor. A
        // margem de um metro absorve o arredondamento do GPS.
        val full = splits.filter { it.distanceM >= 999.0 }
        // Sem quilómetros completos que cheguem não há recorde a esta distância.
        if (full.size < km) return null
        return full.take(km).sumOf { it.movingMs }
    }
}

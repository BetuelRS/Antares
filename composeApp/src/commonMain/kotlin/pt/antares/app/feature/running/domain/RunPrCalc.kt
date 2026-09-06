package pt.antares.app.feature.running.domain

/**
 * Recordes de corrida por distância. O tempo dos primeiros N quilómetros, e não o melhor
 * troço da corrida: é uma aproximação, mas comparável entre atividades.
 */
object RunPrCalc {

    fun bestTimeMs(activities: List<List<Split>>, km: Int): Long? =
        activities.mapNotNull { timeForKm(it, km) }.minOrNull()

    fun timeForKm(splits: List<Split>, km: Int): Long? {

        // As voltas marcadas à mão ficam de fora, e não é um pormenor: elas vivem na mesma
        // lista dos quilómetros e medem o que a pessoa quis, não mil metros. Sem esta
        // condição, uma volta de 1 200 m entrava na conta como se fosse um quilómetro e o
        // recorde saía por uma distância que ninguém correu.
        //
        // O último parcial de uma corrida está quase sempre incompleto — a corrida acabou
        // a meio do quilómetro — e somá-lo dava um tempo bom por uma distância menor. A
        // margem de um metro absorve o arredondamento do GPS.
        val full = splits.filter { !it.manual && it.distanceM >= 999.0 }
        // Sem quilómetros completos que cheguem não há recorde a esta distância.
        if (full.size < km) return null
        return full.take(km).sumOf { it.movingMs }
    }
}

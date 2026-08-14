package pt.antares.app.feature.running.domain

/**
 * Reduz o percurso antes de o codificar. Cinco metros é a ordem de grandeza do erro do
 * próprio GPS: pontos mais próximos do que isso são ruído do sensor, não movimento, e
 * desenham o mesmo traço com uma fração dos dados.
 *
 * Não toca na distância registada — essa vem das métricas, calculadas sobre os pontos
 * todos. Isto é só o que se guarda para desenhar.
 */
object TrackPruner {

    fun prune(points: List<Pair<Double, Double>>, minMeters: Double = 5.0): List<Pair<Double, Double>> {
        if (points.size <= 2) return points
        // Primeiro e último ficam sempre: são o início e o fim da corrida, e cortá-los
        // encurtava o traço nas pontas.
        val out = mutableListOf(points.first())
        var (lastLat, lastLon) = points.first()
        for (i in 1 until points.size - 1) {
            val (lat, lon) = points[i]
            // A distância mede-se contra o último ponto guardado e não contra o anterior:
            // assim uma sequência de passos curtos acumula até valer a pena guardar um.
            if (haversine(lastLat, lastLon, lat, lon) >= minMeters) {
                out.add(lat to lon)
                lastLat = lat; lastLon = lon
            }
        }
        out.add(points.last())
        return out
    }
}

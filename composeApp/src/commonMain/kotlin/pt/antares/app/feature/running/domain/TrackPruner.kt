package pt.antares.app.feature.running.domain

object TrackPruner {

    fun prune(points: List<Pair<Double, Double>>, minMeters: Double = 5.0): List<Pair<Double, Double>> {
        if (points.size <= 2) return points
        val out = mutableListOf(points.first())
        var (lastLat, lastLon) = points.first()
        for (i in 1 until points.size - 1) {
            val (lat, lon) = points[i]
            if (haversine(lastLat, lastLon, lat, lon) >= minMeters) {
                out.add(lat to lon)
                lastLat = lat; lastLon = lon
            }
        }
        out.add(points.last())
        return out
    }
}

package pt.antares.app.feature.running.domain

import kotlin.math.roundToInt

/**
 * O formato de polyline codificada dos mapas da Google, implementado à mão por ser preciso
 * no código comum. Guarda um percurso numa string em vez de milhares de linhas.
 *
 * Assenta em duas ideias: só se guarda a diferença para o ponto anterior, que é um número
 * pequeno, e cada número vai em grupos de cinco bits deslocados para o intervalo
 * imprimível do ASCII. Cinco casas decimais dão cerca de um metro de resolução, que é o
 * suficiente para o traço e menos do que o erro do GPS.
 */
object PolylineCodec {

    fun encode(points: List<Pair<Double, Double>>): String {
        val sb = StringBuilder()
        var lastLat = 0L
        var lastLon = 0L
        for ((lat, lon) in points) {
            val iLat = (lat * 1e5).roundToInt().toLong()
            val iLon = (lon * 1e5).roundToInt().toLong()
            encodeValue(iLat - lastLat, sb)
            encodeValue(iLon - lastLon, sb)
            lastLat = iLat
            lastLon = iLon
        }
        return sb.toString()
    }

    fun decode(encoded: String): List<Pair<Double, Double>> {
        val result = mutableListOf<Pair<Double, Double>>()
        var index = 0
        var lat = 0L
        var lon = 0L
        while (index < encoded.length) {
            var shift = 0
            var value = 0L
            var b: Int
            do {
                b = encoded[index++].code - 63
                value = value or ((b and 0x1f).toLong() shl shift)
                shift += 5
            } while (b >= 0x20)
            lat += if (value and 1L != 0L) (value shr 1).inv() else (value shr 1)

            shift = 0
            value = 0L
            do {
                b = encoded[index++].code - 63
                value = value or ((b and 0x1f).toLong() shl shift)
                shift += 5
            } while (b >= 0x20)
            lon += if (value and 1L != 0L) (value shr 1).inv() else (value shr 1)

            result.add(lat / 1e5 to lon / 1e5)
        }
        return result
    }

    private fun encodeValue(v: Long, sb: StringBuilder) {

        // Deslocar e inverter põe o sinal no bit mais baixo, o que faz os negativos
        // pequenos ocuparem tão poucos caracteres como os positivos pequenos.
        var value = if (v < 0) (v shl 1).inv() else (v shl 1)
        while (value >= 0x20) {
            sb.append(((0x20 or (value.toInt() and 0x1f)) + 63).toChar())
            value = value shr 5
        }
        sb.append((value.toInt() + 63).toChar())
    }
}

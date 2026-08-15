package pt.antares.app.feature.running.domain

import pt.antares.app.core.calc.MetCalc
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Transforma amostras de GPS em distância, tempo e ritmo. É uma máquina de estado com
 * memória: cada amostra entra e sai o retrato atualizado da corrida.
 *
 * Guarda uma âncora — o último ponto que contou como movimento — em vez de comparar com a
 * amostra anterior. É essa âncora que faz o ruído de um telemóvel parado não somar
 * distância: dois metros para cada lado a cada segundo nunca chegam ao mínimo.
 *
 * Puro e sem dependências de plataforma, para poder ser testado com uma lista de pontos.
 */
class RunEngine(
    private val type: ActivityType,
    private val weightKg: Double,
    private val autoPauseEnabled: Boolean = true,
) {
    private companion object {
        // Amostras com erro acima de 30 m são descartadas: vêm de dentro de casa ou de um
        // túnel, e saltam centenas de metros de uma vez.
        const val MAX_ACC_M = 30.0
        // Abaixo de 3 m do ponto âncora não conta como deslocação — é o tremor do sensor.
        const val MIN_MOVE_M = 3.0
        // Pausa automática: dez segundos parado abaixo de meio metro por segundo. O tempo
        // de espera evita pausar num semáforo de dois segundos.
        const val AUTO_PAUSE_SPEED = 0.5
        const val AUTO_PAUSE_HOLD_MS = 10_000L
        const val ELEV_WINDOW = 5
        const val EARTH_R = 6_371_000.0

        // Correr custa cerca de uma caloria por quilo e por quilómetro, quase
        // independentemente do ritmo; andar custa pouco mais de metade. De bicicleta não há
        // regra por distância — a resistência do ar domina —, e por isso essa passa pelo MET
        // da velocidade. Os três valores são brutos: o repouso sai depois, no `liquido`.
        const val RUN_KCAL_PER_KG_KM = 1.0
        const val WALK_KCAL_PER_KG_KM = 0.53
    }

    // Teto de velocidade plausível, em metros por segundo: acima disto é um salto do GPS e
    // não uma pessoa. Mais alto de bicicleta, que é mais rápida do que qualquer corredor.
    private val maxSpeed = if (type == ActivityType.RIDE) 25.0 else 12.0

    private var anchorLat = 0.0
    private var anchorLon = 0.0
    private var anchorT = 0L
    private var firstT = 0L
    private var lastT = 0L
    private var started = false

    private var distanceM = 0.0
    private var movingMs = 0L
    private var curSpeedMps = 0.0
    private var kcalRide = 0.0
    private var paused = false

    private val elevWindow = ArrayDeque<Double>()
    private var smoothedAlt: Double? = null
    private var elevGainM = 0.0

    private val splits = mutableListOf<Split>()
    private var lastSplitDist = 0.0
    private var lastSplitMoving = 0L

    fun onSample(s: GeoSample): RunMetrics {

        if (s.accM > MAX_ACC_M) return metrics()

        if (!started) {
            started = true
            firstT = s.tMs; lastT = s.tMs
            anchorLat = s.lat; anchorLon = s.lon; anchorT = s.tMs
            pushElevation(s.altM)
            return metrics()
        }

        // Amostra fora de ordem ou repetida: os fornecedores de localização entregam-nas
        // assim de vez em quando, e um intervalo negativo estragava tempo e velocidade.
        if (s.tMs <= lastT) return metrics()

        val dFromAnchor = haversine(anchorLat, anchorLon, s.lat, s.lon)
        val dtFromAnchorMs = s.tMs - anchorT
        val segSpeed = if (dtFromAnchorMs > 0) dFromAnchor / (dtFromAnchorMs / 1000.0) else 0.0

        // Salto impossível: descarta-se a amostra sem mexer na âncora, para a seguinte ser
        // comparada com o último ponto bom em vez de com o salto.
        if (segSpeed > maxSpeed) {

            return metrics()
        }

        val dtSample = s.tMs - lastT
        val movingBefore = movingMs
        if (!autoPauseEnabled || !paused) movingMs += dtSample
        lastT = s.tMs

        // De bicicleta as calorias acumulam-se amostra a amostra, porque o MET depende da
        // velocidade do momento. A correr e a andar bastam a distância e o peso no fim.
        if (type == ActivityType.RIDE && (!autoPauseEnabled || !paused)) {
            kcalRide += metForCycling(curSpeedMps) * weightKg * (dtSample / 3_600_000.0)
        }

        if (dFromAnchor >= MIN_MOVE_M) {

            addDistanceWithSplits(
                fromDist = distanceM,
                segDist = dFromAnchor,
                segMovingStart = movingBefore,
                dtMovingMs = movingMs - movingBefore,
            )
            distanceM += dFromAnchor
            curSpeedMps = segSpeed
            pushElevation(s.altM)
            anchorLat = s.lat; anchorLon = s.lon; anchorT = s.tMs
            paused = false
        } else {

            curSpeedMps = segSpeed
            val idleMs = s.tMs - anchorT
            if (autoPauseEnabled && idleMs >= AUTO_PAUSE_HOLD_MS && segSpeed < AUTO_PAUSE_SPEED) {
                paused = true
            }
        }
        return metrics()
    }

    fun finish(): RunResult {
        // Fecha o quilómetro incompleto do fim. Fica na lista marcado pela distância real,
        // que é como o [RunPrCalc] o distingue dos completos ao calcular recordes.
        val partial = distanceM - lastSplitDist
        if (partial > 1.0) {
            val dt = movingMs - lastSplitMoving
            splits += Split(
                index = splits.size + 1,
                distanceM = partial,
                movingMs = dt,
                paceSecPerKm = paceSecPerKm(partial, dt),
                kcal = splitKcal(partial, dt).roundToInt(),
            )
        }
        return RunResult(metrics = metrics(), splits = splits.toList())
    }

    /**
     * Fecha os quilómetros que este segmento atravessa. `while` e não `if`: com o GPS a
     * falhar um pedaço, um único segmento pode cruzar várias fronteiras de uma vez.
     */
    private fun addDistanceWithSplits(fromDist: Double, segDist: Double, segMovingStart: Long, dtMovingMs: Long) {
        var boundary = ((fromDist / 1000.0).toInt() + 1) * 1000.0
        while (fromDist + segDist >= boundary) {
            val distInto = boundary - fromDist
            // Interpola o instante da passagem dentro do segmento, assumindo velocidade
            // constante nele. Sem isto, o tempo do parcial saltaria para o fim do segmento
            // e os parciais ficavam todos deslocados.
            val f = distInto / segDist
            val movingAtCross = segMovingStart + (f * dtMovingMs).toLong()
            val splitDist = boundary - lastSplitDist
            val splitMoving = movingAtCross - lastSplitMoving
            splits += Split(
                index = splits.size + 1,
                distanceM = splitDist,
                movingMs = splitMoving,
                paceSecPerKm = paceSecPerKm(splitDist, splitMoving),
                kcal = splitKcal(splitDist, splitMoving).roundToInt(),
            )
            lastSplitDist = boundary
            lastSplitMoving = movingAtCross
            boundary += 1000.0
        }
    }

    /**
     * Acumula o desnível positivo sobre uma média móvel de cinco amostras. A altitude do
     * GPS oscila metros a cada leitura mesmo em terreno plano; somar as subidas cruas dava
     * centenas de metros de desnível a quem correu à beira-mar.
     */
    private fun pushElevation(alt: Double?) {
        if (alt == null) return
        elevWindow.addLast(alt)
        if (elevWindow.size > ELEV_WINDOW) elevWindow.removeFirst()
        val avg = elevWindow.average()
        val prev = smoothedAlt
        // Só as subidas contam: é desnível positivo acumulado, e a descida não o desfaz.
        if (prev != null) elevGainM += max(0.0, avg - prev)
        smoothedAlt = avg
    }

    private fun splitKcal(distM: Double, movingMsSeg: Long): Double {
        val bruto = when (type) {
            ActivityType.RUN -> RUN_KCAL_PER_KG_KM * weightKg * (distM / 1000.0)
            ActivityType.WALK -> WALK_KCAL_PER_KG_KM * weightKg * (distM / 1000.0)

            ActivityType.RIDE -> {
                val speed = if (movingMsSeg > 0) distM / (movingMsSeg / 1000.0) else 0.0
                metForCycling(speed) * weightKg * (movingMsSeg / 3_600_000.0)
            }
        }
        return liquido(bruto, movingMsSeg)
    }

    private fun totalKcal(movingMsVal: Long): Int {
        val bruto = when (type) {
            ActivityType.RUN -> RUN_KCAL_PER_KG_KM * weightKg * (distanceM / 1000.0)
            ActivityType.WALK -> WALK_KCAL_PER_KG_KM * weightKg * (distanceM / 1000.0)
            ActivityType.RIDE -> kcalRide
        }
        return liquido(bruto, movingMsVal).roundToInt()
    }

    /**
     * O que a corrida gastou **a mais** do que estar parado.
     *
     * As contas por quilómetro e por MET dão o gasto total do período, repouso incluído, e
     * o repouso já está na meta diária — somá-lo ao orçamento contava-o duas vezes. Num
     * quilómetro a correr o desconto anda pelos 9%; a andar, mais.
     *
     * Aplica-se aos parciais e ao total pela mesma via, senão os quilómetros no ecrã
     * deixavam de somar o número grande.
     */
    private fun liquido(bruto: Double, movingMsSeg: Long): Double {
        val repouso = MetCalc.REST_MET * weightKg * (movingMsSeg / 3_600_000.0)
        return max(0.0, bruto - repouso)
    }

    private fun metrics(): RunMetrics {
        val elapsed = if (started) lastT - firstT else 0L
        // Sem pausa automática, tempo em movimento é o tempo decorrido: não há como
        // distinguir os dois, e mantê-los separados daria um ritmo médio inflacionado.
        val moving = if (autoPauseEnabled) movingMs else elapsed
        return RunMetrics(
            distanceM = distanceM,
            elapsedMs = elapsed,
            movingMs = moving,
            avgPaceSecPerKm = paceSecPerKm(distanceM, moving),
            curSpeedMps = curSpeedMps,
            kcal = totalKcal(moving),
            elevGainM = elevGainM,
            paused = paused,
        )
    }

    private fun paceSecPerKm(distM: Double, movingMsVal: Long): Int {
        // Zero abaixo de um metro: o ritmo por quilómetro de uma distância quase nula é um
        // número enorme e sem sentido, e o ecrã trata o zero como "ainda não há ritmo".
        if (distM < 1.0) return 0
        val km = distM / 1000.0
        val sec = movingMsVal / 1000.0
        return (sec / km).roundToInt()
    }
}

internal fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val p1 = lat1 * PI / 180.0
    val p2 = lat2 * PI / 180.0
    val dp = (lat2 - lat1) * PI / 180.0
    val dl = (lon2 - lon1) * PI / 180.0
    val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

internal fun metForCycling(speedMps: Double): Double {
    val kmh = speedMps * 3.6
    return when {
        kmh < 16 -> 4.0
        kmh < 19 -> 6.8
        kmh < 22 -> 8.0
        kmh < 25 -> 10.0
        kmh < 30 -> 12.0
        else -> 15.8
    }
}

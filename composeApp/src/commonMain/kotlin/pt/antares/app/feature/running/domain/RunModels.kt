package pt.antares.app.feature.running.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ActivityType { RUN, WALK, RIDE }

@Serializable
enum class RunStatus { DONE, DISCARDED }

data class GeoSample(
    val tMs: Long,
    val lat: Double,
    val lon: Double,
    val altM: Double? = null,
    val accM: Double = 0.0,
    val speedMps: Double? = null,
)

@Serializable
data class Split(
    val index: Int,
    val distanceM: Double,
    val movingMs: Long,
    val paceSecPerKm: Int,
    val kcal: Int,

    /**
     * Se este parcial foi **marcado à mão** em vez de fechado por um quilómetro.
     *
     * Nasce com omissão, e é isso que faz as corridas já gravadas continuarem a abrir: as
     * parciais viajam num `splitsJson` dentro da corrida, e um campo sem omissão parava a
     * leitura de tudo o que foi gravado antes dele. É a lição do `RotinaDeCopiaAntigaTest`,
     * noutro formato.
     *
     * As duas séries partilham a lista e **não partilham a numeração**: o quilómetro 2 é o
     * segundo quilómetro, e a volta 2 é a segunda volta. Uma volta marcada a meio não
     * renumera os quilómetros que vêm a seguir.
     */
    val manual: Boolean = false,
)

data class RunMetrics(
    val distanceM: Double = 0.0,
    val elapsedMs: Long = 0L,
    val movingMs: Long = 0L,
    val avgPaceSecPerKm: Int = 0,
    val curSpeedMps: Double = 0.0,
    val kcal: Int = 0,
    val elevGainM: Double = 0.0,

    /** A pausa automática: dez segundos parado. Apaga-se sozinha ao primeiro passo. */
    val paused: Boolean = false,

    /** A pausa que a pessoa pediu, que só ela desfaz. */
    val pausaManual: Boolean = false,

    /**
     * O ritmo do quilómetro que vai a meio, em segundos por quilómetro. Zero enquanto não
     * houver um metro andado desde o último parcial — o ecrã trata o zero como «ainda não».
     */
    val ritmoDoKmSecPerKm: Int = 0,
)

data class RunResult(
    val metrics: RunMetrics,
    val splits: List<Split>,
)

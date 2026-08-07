package pt.antares.app.core.demo

import pt.antares.app.core.database.entities.BodyMeasurementEntity
import pt.antares.app.core.database.entities.FastingSessionEntity
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.database.entities.WaterLogEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.FastingStatus
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.core.model.WeightSource
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.RunStatus
import kotlin.math.abs
import kotlin.math.roundToInt

const val DEMO_ID_PREFIX = "demo-"

data class DemoFood(
    val id: String,
    val nome: String,
    val kcalPer100: Int,
    val proteinaPer100: Double,
    val hidratosPer100: Double,
    val gorduraPer100: Double,
    val liquido: Boolean = false,
)

data class DemoData(
    val pesos: List<WeightLogEntity>,
    val medidas: List<BodyMeasurementEntity>,
    val refeicoes: List<FoodLogEntity>,
    val aguas: List<WaterLogEntity>,
    val treinos: List<WorkoutSessionEntity>,
    val series: List<WorkoutSetEntity>,
    val corridas: List<RunEntity>,
    val jejuns: List<FastingSessionEntity>,
) {
    val total: Int
        get() = pesos.size + medidas.size + refeicoes.size + aguas.size +
            treinos.size + series.size + corridas.size + jejuns.size
}

object DemoDataEngine {

    const val DIAS = 730

    const val SEMENTE_PADRAO = 20260803L

    private const val KCAL_POR_KG = 7700.0

    private const val ALTURA_CM = 178.0
    private const val IDADE = 34
    private const val FATOR_ATIVIDADE = 1.5

    private const val MS_POR_DIA = 86_400_000L

    private val ANCORAS = listOf(
        0 to 110.0,
        60 to 105.5,
        120 to 102.0,
        180 to 101.5,
        240 to 97.0,
        300 to 99.5,
        360 to 95.0,
        420 to 89.0,
        480 to 87.5,
        540 to 82.0,
        600 to 84.0,
        660 to 76.0,
        700 to 71.5,
        DIAS to 70.0,
    )

    private fun pesoBase(dia: Int): Double {
        if (dia <= 0) return ANCORAS.first().second
        if (dia >= DIAS) return ANCORAS.last().second
        val depois = ANCORAS.first { it.first >= dia }
        val antes = ANCORAS.last { it.first <= dia }
        if (antes.first == depois.first) return antes.second
        val fracao = (dia - antes.first).toDouble() / (depois.first - antes.first)
        return antes.second + (depois.second - antes.second) * fracao
    }

    private fun kcalDoDia(dia: Int): Int {
        val peso = pesoBase(dia)
        val bmr = 10 * peso + 6.25 * ALTURA_CM - 5 * IDADE + 5
        val manutencao = bmr * FATOR_ATIVIDADE
        val declive = pesoBase(dia + 1) - pesoBase(dia)
        return (manutencao + declive * KCAL_POR_KG).roundToInt().coerceIn(1400, 4000)
    }

    fun gerar(
        semente: Long = SEMENTE_PADRAO,
        diaFinal: Long,
        catalogo: List<DemoFood> = emptyList(),
        exercicios: List<String> = emptyList(),
        protocoloJejumId: String? = null,
    ): DemoData {
        val r = DemoRandom(semente)
        val pesos = mutableListOf<WeightLogEntity>()
        val medidas = mutableListOf<BodyMeasurementEntity>()
        val refeicoes = mutableListOf<FoodLogEntity>()
        val aguas = mutableListOf<WaterLogEntity>()
        val treinos = mutableListOf<WorkoutSessionEntity>()
        val series = mutableListOf<WorkoutSetEntity>()
        val corridas = mutableListOf<RunEntity>()
        val jejuns = mutableListOf<FastingSessionEntity>()

        for (i in 0..DIAS) {
            val dia = diaFinal - (DIAS - i)
            val quando = dia * MS_POR_DIA
            val peso = pesoBase(i)

            if (r.chance(0.72)) {

                val lido = peso + r.entre(-0.6, 0.6)
                pesos += WeightLogEntity(
                    id = "${DEMO_ID_PREFIX}weight-$dia",
                    epochDay = dia,
                    weightKg = arredonda(lido, 1),
                    note = null,
                    source = WeightSource.MANUAL,
                    sourceRef = null,
                    updatedAt = quando,
                    deleted = false,
                    dirty = false,
                )
            }

            if (i % 14 == 0) {
                val gordura = gorduraPct(peso)
                medidas += BodyMeasurementEntity(
                    id = "${DEMO_ID_PREFIX}measure-$dia",
                    epochDay = dia,
                    bodyFatPct = arredonda(gordura, 1),
                    bodyFatSource = BodyFatSource.NAVY,

                    waistCm = arredonda(72 + (peso - 70) * 0.9, 1),
                    neckCm = arredonda(36 + (peso - 70) * 0.12, 1),
                    hipCm = arredonda(94 + (peso - 70) * 0.55, 1),

                    armCm = arredonda(33 + (DIAS - i) / 730.0 * -2.5 + i / 730.0 * 3.5, 1),
                    thighCm = arredonda(56 + (peso - 70) * 0.35, 1),
                    chestCm = arredonda(100 + (peso - 70) * 0.4, 1),
                    updatedAt = quando,
                    deleted = false,
                    dirty = false,
                )
            }

            val registou = catalogo.isNotEmpty() && r.chance(0.88)
            if (registou) {
                val alvo = kcalDoDia(i) * r.entre(0.93, 1.07)
                refeicoes += refeicoesDoDia(r, dia, quando, alvo, catalogo)
            }

            if (r.chance(0.8)) {
                aguas += WaterLogEntity(
                    id = "${DEMO_ID_PREFIX}water-$dia",
                    epochDay = dia,
                    ml = r.inteiroEntre(6, 12) * 250,
                    updatedAt = quando,
                    deleted = false,
                    dirty = false,
                )
            }

            val diaDaSemana = (((dia % 7) + 7) % 7).toInt()
            if (diaDaSemana in setOf(1, 3, 5) && exercicios.isNotEmpty() && r.chance(0.88)) {
                val sessaoId = "${DEMO_ID_PREFIX}session-$dia"
                val inicio = quando + 18 * 3_600_000L
                val duracao = r.inteiroEntre(45, 80) * 60_000L
                treinos += WorkoutSessionEntity(
                    id = sessaoId,
                    startedAt = inicio,
                    endedAt = inicio + duracao,
                    routineId = null,
                    note = null,
                    status = SessionStatus.DONE,
                    updatedAt = quando,
                    deleted = false,
                    dirty = false,
                )
                series += seriesDoTreino(r, sessaoId, quando, i, exercicios)
            }

            if (diaDaSemana in setOf(2, 6) && i > 60 && r.chance(0.75)) {
                corridas += corrida(r, dia, quando, i, peso)
            }

            if (protocoloJejumId != null && i > 400 && diaDaSemana in setOf(2, 4) && r.chance(0.5)) {
                val inicio = quando + 20 * 3_600_000L
                val alvo = inicio + 16 * 3_600_000L

                val cumpriu = r.chance(0.85)
                jejuns += FastingSessionEntity(
                    id = "${DEMO_ID_PREFIX}fast-$dia",
                    protocolId = protocoloJejumId,
                    startedAt = inicio,
                    targetEndAt = alvo,
                    endedAt = if (cumpriu) alvo + r.inteiroEntre(0, 90) * 60_000L else inicio + r.inteiroEntre(9, 14) * 3_600_000L,
                    status = if (cumpriu) FastingStatus.COMPLETED else FastingStatus.BROKEN,
                    updatedAt = quando,
                    deleted = false,
                    dirty = false,
                )
            }
        }

        return DemoData(pesos, medidas, refeicoes, aguas, treinos, series, corridas, jejuns)
    }

    private fun gorduraPct(peso: Double): Double {
        val imc = peso / ((ALTURA_CM / 100) * (ALTURA_CM / 100))
        return (1.20 * imc + 0.23 * IDADE - 16.2).coerceIn(8.0, 45.0)
    }

    private fun refeicoesDoDia(
        r: DemoRandom,
        dia: Long,
        quando: Long,
        alvoKcal: Double,
        catalogo: List<DemoFood>,
    ): List<FoodLogEntity> {

        val reparticao = listOf(
            MealSlot.BREAKFAST to 0.22,
            MealSlot.LUNCH to 0.34,
            MealSlot.DINNER to 0.32,
            MealSlot.SNACK to 0.12,
        )
        val saida = mutableListOf<FoodLogEntity>()
        for ((slot, parte) in reparticao) {
            val kcalRefeicao = alvoKcal * parte
            val quantos = r.inteiroEntre(1, 3)
            for (n in 0 until quantos) {
                val alimento = r.um(catalogo) ?: continue

                val kcalItem = kcalRefeicao / quantos
                if (alimento.kcalPer100 <= 0) continue
                val gramas = (kcalItem / alimento.kcalPer100 * 100).coerceIn(15.0, 600.0)
                val fator = gramas / 100.0
                saida += FoodLogEntity(
                    id = "${DEMO_ID_PREFIX}log-$dia-${slot.name.lowercase()}-$n",
                    epochDay = dia,
                    mealSlot = slot,
                    foodId = alimento.id,
                    nameSnapshot = alimento.nome,
                    quantityGrams = arredonda(gramas, 0),
                    kcalSnapshot = (alimento.kcalPer100 * fator).roundToInt(),
                    proteinSnapshot = arredonda(alimento.proteinaPer100 * fator, 1),
                    carbsSnapshot = arredonda(alimento.hidratosPer100 * fator, 1),
                    fatSnapshot = arredonda(alimento.gorduraPer100 * fator, 1),
                    microsPer100Json = null,
                    origin = LogOrigin.MANUAL,
                    isLiquid = alimento.liquido,
                    updatedAt = quando,
                    deleted = false,
                    dirty = false,
                )
            }
        }
        return saida
    }

    private fun seriesDoTreino(
        r: DemoRandom,
        sessaoId: String,
        quando: Long,
        dia: Int,
        exercicios: List<String>,
    ): List<WorkoutSetEntity> {
        val saida = mutableListOf<WorkoutSetEntity>()
        val quantosExercicios = r.inteiroEntre(4, 6)

        val progresso = dia.toDouble() / DIAS
        for (e in 0 until quantosExercicios) {
            val exercicioId = r.um(exercicios) ?: continue

            val base = 20.0 + (abs(exercicioId.hashCode()) % 60)
            val carga = base * (1.0 + progresso * 0.45)
            val quantasSeries = r.inteiroEntre(3, 4)
            for (s in 0 until quantasSeries) {
                saida += WorkoutSetEntity(
                    id = "$sessaoId-set-$e-$s",
                    sessionId = sessaoId,
                    exerciseId = exercicioId,
                    setIndex = s,

                    weightKg = arredonda(carga + s * 2.5, 1),
                    reps = (12 - s - r.ate(2)).coerceAtLeast(4),
                    rpe = if (r.chance(0.4)) arredonda(r.entre(6.0, 9.5), 1) else null,
                    isWarmup = s == 0,
                    updatedAt = quando,
                    deleted = false,
                    dirty = false,
                )
            }
        }
        return saida
    }

    private fun corrida(r: DemoRandom, dia: Long, quando: Long, i: Int, peso: Double): RunEntity {
        val progresso = ((i - 60).toDouble() / (DIAS - 60)).coerceIn(0.0, 1.0)
        val distanciaKm = (3.0 + progresso * 7.0) * r.entre(0.85, 1.15)

        val ritmoSegPorKm = (450 - progresso * 110).roundToInt() + r.inteiroEntre(-20, 20)
        val movimentoS = (distanciaKm * ritmoSegPorKm).toLong()
        val inicio = quando + 8 * 3_600_000L
        return RunEntity(
            id = "${DEMO_ID_PREFIX}run-$dia",
            type = ActivityType.RUN,
            startedAt = inicio,
            endedAt = inicio + movimentoS * 1000,
            distanceM = arredonda(distanciaKm * 1000, 0),
            movingS = movimentoS,
            elapsedS = movimentoS + r.inteiroEntre(0, 240),
            avgPaceSecPerKm = ritmoSegPorKm,

            kcal = (distanciaKm * peso).roundToInt(),
            elevGainM = arredonda(r.entre(5.0, 90.0), 0),

            polyline = "",
            splitsJson = "[]",
            name = "",
            note = "",
            status = RunStatus.DONE,
            updatedAt = quando,
            deleted = false,
            dirty = false,
        )
    }

    private fun arredonda(valor: Double, casas: Int): Double {
        var fator = 1.0
        repeat(casas) { fator *= 10 }
        return (valor * fator).roundToInt() / fator
    }
}

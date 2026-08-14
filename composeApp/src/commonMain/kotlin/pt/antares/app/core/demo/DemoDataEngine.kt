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

// Todos os identificadores gerados aqui começam por isto. É a única maneira de os apagar
// depois sem tocar em nada do utilizador, e o DemoDataEngineTest falha se algum escapar.
const val DEMO_ID_PREFIX = "demo-"

data class DemoFood(
    val id: String,
    val nome: String,
    val kcalPer100: Int,
    val proteinaPer100: Double,
    val hidratosPer100: Double,
    val gorduraPer100: Double,
    val liquido: Boolean = false,

    /**
     * Micros por 100 g, tal como vêm do catálogo. Sem eles a cobertura EFSA, o
     * "falta hoje" e as lacunas do relatório semanal ficam vazios mesmo com dois
     * anos de registos.
     */
    val microsJson: String? = null,
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

/**
 * Inventa dois anos de uso da app. Puro: recebe tudo o que precisa e devolve entidades sem
 * tocar na base — quem escreve é o [DemoDataWriter], e é isso que o torna testável.
 *
 * A pessoa fictícia perde 40 kg em dois anos, mas não em linha reta: há paragens e
 * recuperações de propósito, porque uma descida perfeita não exercitaria o plateau, a
 * proposta adaptativa nem a pausa de dieta.
 */
object DemoDataEngine {

    // Dois anos. Chega para o histórico anual e para várias janelas de tendência.
    const val DIAS = 730

    // Semente fixa: a mesma demonstração de cada vez. Sem isto, comparar dois ecrãs depois
    // de regerar não provava nada.
    const val SEMENTE_PADRAO = 20260803L

    private const val KCAL_POR_KG = 7700.0

    // Perfil da pessoa fictícia, fixo aqui e não lido do perfil real: a demonstração tem de
    // dar os mesmos números a quem quer que a ligue.
    private const val ALTURA_CM = 178.0
    private const val IDADE = 34
    private const val FATOR_ATIVIDADE = 1.5

    private const val MS_POR_DIA = 86_400_000L

    // Pontos por onde o peso passa. As subidas — aos dias 300 e 600 — são deliberadas: sem
    // elas não haveria plateaus para os ecrãs mostrarem.
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

    /** Interpola linearmente entre âncoras: o peso "verdadeiro" desse dia, sem ruído. */
    private fun pesoBase(dia: Int): Double {
        if (dia <= 0) return ANCORAS.first().second
        if (dia >= DIAS) return ANCORAS.last().second
        val depois = ANCORAS.first { it.first >= dia }
        val antes = ANCORAS.last { it.first <= dia }
        if (antes.first == depois.first) return antes.second
        val fracao = (dia - antes.first).toDouble() / (depois.first - antes.first)
        return antes.second + (depois.second - antes.second) * fracao
    }

    /**
     * As calorias saem da curva de peso, e não ao contrário. É o que faz a demonstração ser
     * coerente: o [AdaptiveTdee] a ler estes dados chega a um gasto que bate certo com a
     * perda desenhada, em vez de ver ruído.
     */
    private fun kcalDoDia(dia: Int): Int {
        val peso = pesoBase(dia)
        // Mifflin escrita à mão em vez de chamar o [NutritionCalc]: a demonstração não pode
        // mudar quando as fórmulas da app mudarem, ou deixa de ser reproduzível.
        val bmr = 10 * peso + 6.25 * ALTURA_CM - 5 * IDADE + 5
        val manutencao = bmr * FATOR_ATIVIDADE
        // O declive do dia seguinte convertido em calorias dá o défice ou o excedente que
        // explica o movimento do peso.
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

            // Nem toda a gente se pesa todos os dias, e a app tem de aguentar buracos: as
            // probabilidades deste ciclo existem para a demonstração ter falhas realistas.
            if (r.chance(0.72)) {

                // Ruído de ±600 g sobre o peso verdadeiro: é a variação de água e digestão
                // que a suavização do [WeightTrend] existe para atravessar.
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

            // Medidas de fita de duas em duas semanas, que é a cadência a que fazem sentido.
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

                    // O braço cresce enquanto o resto encolhe: é o que faz a demonstração
                    // mostrar recomposição em vez de só perda de peso.
                    armCm = arredonda(33 + (DIAS - i) / 730.0 * -2.5 + i / 730.0 * 3.5, 1),
                    thighCm = arredonda(56 + (peso - 70) * 0.35, 1),
                    chestCm = arredonda(100 + (peso - 70) * 0.4, 1),
                    updatedAt = quando,
                    deleted = false,
                    dirty = false,
                )
            }

            // Sem catálogo semeado não se inventam alimentos: os registos apontariam a
            // identificadores que não existem, e o diário ficaria com nomes sem alimento.
            val registou = catalogo.isNotEmpty() && r.chance(0.88)
            if (registou) {
                // ±7% à volta do alvo. Sem esta folga, o gasto observado saía exato e a
                // proposta adaptativa nunca teria nada para corrigir.
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

            // Treinos, corridas e jejuns caem em dias fixos da semana: um padrão semanal é
            // o que os ecrãs de volume e de hábitos precisam de ter para dizer alguma coisa.
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

            // As corridas só começam ao fim de dois meses e os jejuns ao fim de mais de um
            // ano: os hábitos aparecem ao longo da história em vez de existirem desde o
            // primeiro dia, e é isso que dá aos gráficos um princípio.
            if (diaDaSemana in setOf(2, 6) && i > 60 && r.chance(0.75)) {
                corridas += corrida(r, dia, quando, i, peso)
            }

            if (protocoloJejumId != null && i > 400 && diaDaSemana in setOf(2, 4) && r.chance(0.5)) {
                val inicio = quando + 20 * 3_600_000L
                val alvo = inicio + 16 * 3_600_000L

                // Um em cada sete jejuns falha, para o histórico ter sessões interrompidas e
                // a taxa de cumprimento não ser sempre 100%.
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

    // Deurenberg escrita à mão, como a Mifflin acima: a demonstração não pode mudar quando
    // as fórmulas da app mudarem.
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

        // Repartição fixa pelas refeições. Nenhuma passa de 45% do dia, para a demonstração
        // não disparar o padrão de concentração do [EatingPatterns].
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
                // Limites de porção plausível. Sem eles, um alimento de poucas calorias por
                // 100 g dava porções de quilos para chegar ao alvo da refeição.
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
                    microsPer100Json = alimento.microsJson,
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

            // A carga inicial deriva do identificador do exercício, e não do acaso: assim
            // cada exercício tem sempre o seu peso próprio ao longo dos dois anos, e o
            // histórico de um exercício não salta de 30 para 80 kg entre treinos.
            val base = 20.0 + (abs(exercicioId.hashCode()) % 60)
            // 45% de progressão em dois anos, para os recordes irem caindo ao longo da
            // demonstração em vez de aparecerem todos no primeiro treino.
            val carga = base * (1.0 + progresso * 0.45)
            val quantasSeries = r.inteiroEntre(3, 4)
            for (s in 0 until quantasSeries) {
                saida += WorkoutSetEntity(
                    id = "$sessaoId-set-$e-$s",
                    sessionId = sessaoId,
                    exerciseId = exercicioId,
                    setIndex = s,

                    // Peso a subir e repetições a descer ao longo das séries, como num
                    // treino real depois do aquecimento.
                    weightKg = arredonda(carga + s * 2.5, 1),
                    reps = (12 - s - r.ate(2)).coerceAtLeast(4),
                    // RPE em menos de metade das séries: quase ninguém o preenche sempre, e
                    // os ecrãs têm de saber lidar com a falta.
                    rpe = if (r.chance(0.4)) arredonda(r.entre(6.0, 9.5), 1) else null,
                    // A primeira série é aquecimento, e por isso não conta para o volume.
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

        // Distância a subir e ritmo a descer com o tempo: de 7:30 para cerca de 5:40 ao
        // quilómetro, que é uma evolução de dois anos plausível para quem começa.
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

            // Aproximação clássica: cerca de uma caloria por quilo e por quilómetro.
            kcal = (distanciaKm * peso).roundToInt(),
            elevGainM = arredonda(r.entre(5.0, 90.0), 0),

            // Sem percurso nem parciais: inventar coordenadas daria um mapa de uma corrida
            // que nunca existiu, num sítio real. É o que deixa o detalhe da corrida com
            // metade do ecrã vazia na demonstração.
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

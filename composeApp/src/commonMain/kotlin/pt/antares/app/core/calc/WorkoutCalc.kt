package pt.antares.app.core.calc

import kotlin.math.roundToInt

object OneRepMax {
    /**
     * Fórmula de Epley. Acima de doze repetições devolve null: a partir daí a série testa
     * resistência e não força, e a estimativa afasta-se do que a pessoa levantaria mesmo.
     */
    fun epley(weightKg: Double, reps: Int): Double? {
        if (weightKg <= 0.0 || reps < 1 || reps > 12) return null
        return weightKg * (1.0 + reps / 30.0)
    }
}

/**
 * Limites do que se aceita numa série.
 *
 * **Este comentário dizia que um engano de dedo aqui não se corrigia** — que um valor
 * absurdo ficava como recorde para sempre. Era verdade quando foi escrito, e o
 * `estudo/transversal/02-robustez.md` cita-o como o exemplo do padrão que mais o incomoda:
 * a app identifica o risco, escreve-o num comentário, e não fecha a saída.
 *
 * Deixou de ser verdade em dois passos. O `updateSet` passou a ser chamado do ecrã da
 * sessão, portanto uma série corrige-se onde se escreveu; e o [PrDetector] **calcula** o
 * melhor a partir das séries em vez de guardar um recorde, portanto corrigir a série corrige
 * o recorde no mesmo instante. Um zero a mais deixa de sobreviver à correcção.
 *
 * O que estes limites ainda fazem é travar o disparate à entrada, para ele nem chegar ao
 * volume por músculo nem à proposta adaptativa. O teto do peso é generoso de propósito — o
 * recorde do mundo de peso morto anda pelos 500 kg, por isso nenhum levantador real lhe
 * chega.
 */
object SetLimits {
    const val MAX_WEIGHT_KG = 500.0

    /** Séries de peso do corpo chegam facilmente às dezenas; 200 é folga com fim. */
    const val MAX_REPS = 200

    const val MIN_RPE = 1.0
    const val MAX_RPE = 10.0

    fun isWeightValid(weightKg: Double): Boolean =
        weightKg.isFinite() && weightKg > 0.0 && weightKg <= MAX_WEIGHT_KG

    fun isRepsValid(reps: Int): Boolean = reps in 1..MAX_REPS

    /** Sem RPE é válido: o campo é opcional. */
    fun isRpeValid(rpe: Double?): Boolean =
        rpe == null || (rpe.isFinite() && rpe >= MIN_RPE && rpe <= MAX_RPE)

    fun isSetValid(weightKg: Double?, reps: Int?, rpe: Double?): Boolean =
        weightKg != null && reps != null &&
            isWeightValid(weightKg) && isRepsValid(reps) && isRpeValid(rpe)
}

data class SetEntry(
    val weightKg: Double,
    val reps: Int,
    val isWarmup: Boolean = false,
)

object VolumeCalc {
    // O aquecimento fica de fora em toda a app: conta-lo inflacionava o volume de quem
    // aquece muito e fazia semanas parecerem mais duras do que foram.
    fun volume(sets: List<SetEntry>): Double =
        sets.filter { !it.isWarmup }.sumOf { it.weightKg * it.reps }
}

/**
 * Os dois recordes que a app persegue: o 1RM estimado, que mede força, e o melhor
 * peso × repetições, que apanha os progressos em séries longas onde o 1RM não chega.
 */
data class ExercisePr(

    // Null quando nenhuma série ficou dentro das doze repetições da Epley.
    val bestOneRm: Double?,
    val bestWeightReps: Double,
)

object PrDetector {

    fun best(sets: List<SetEntry>): ExercisePr? {
        val work = sets.filter { !it.isWarmup && it.weightKg > 0 && it.reps > 0 }
        if (work.isEmpty()) return null
        val bestOneRm = work.mapNotNull { OneRepMax.epley(it.weightKg, it.reps) }.maxOrNull()
        val bestWr = work.maxOf { it.weightKg * it.reps }
        return ExercisePr(bestOneRm = bestOneRm, bestWeightReps = bestWr)
    }

    /** `previous` é o melhor de sempre neste exercício, não o do treino anterior. */
    fun detect(previous: ExercisePr?, current: List<SetEntry>): PrResult {
        val now = best(current) ?: return PrResult(false, false)
        if (previous == null) {

            // Primeira vez que se faz o exercício: qualquer série é recorde. É de propósito
            // — a app celebra o começo, e não haveria nada com que comparar.
            return PrResult(
                newOneRm = (now.bestOneRm ?: 0.0) > 0,
                newWeightReps = now.bestWeightReps > 0,
            )
        }

        val umRmNovo = when {
            now.bestOneRm == null -> false
            // Havia recordes anteriores mas nenhum com 1RM calculável: o primeiro que sai
            // é recorde por não ter concorrência.
            previous.bestOneRm == null -> true
            else -> now.bestOneRm > previous.bestOneRm + EPS
        }
        return PrResult(
            newOneRm = umRmNovo,
            newWeightReps = now.bestWeightReps > previous.bestWeightReps + EPS,
        )
    }

    // A margem impede que o mesmo peso repetido seja anunciado como recorde por causa do
    // último bit de um Double.
    private const val EPS = 1e-6
}

data class PrResult(val newOneRm: Boolean, val newWeightReps: Boolean) {
    val any: Boolean get() = newOneRm || newWeightReps
}

data class MuscleVolumeInput(
    val weightKg: Double,
    val reps: Int,
    val primaryMuscles: List<String>,
)

object MuscleVolume {
    // Balde para exercícios sem músculos declarados, quase sempre criados pelo utilizador.
    // Sem ele o volume desses treinos desaparecia do mapa sem nada o dizer.
    const val OTHER = "other"

    /**
     * Volume por músculo. Um exercício com vários músculos primários soma o volume inteiro
     * a cada um, por isso o total do mapa é maior do que o volume do treino.
     */
    fun aggregate(inputs: List<MuscleVolumeInput>): Map<String, Double> {
        val out = mutableMapOf<String, Double>()
        for (i in inputs) {
            val vol = i.weightKg * i.reps
            val targets = i.primaryMuscles.ifEmpty { listOf(OTHER) }
            // Sem repartir pelo número de músculos: os secundários já ficaram de fora, e
            // dividir subavaliava o trabalho de cada um nos compostos.
            for (m in targets) out[m] = (out[m] ?: 0.0) + vol
        }
        return out
    }
}

/** Uma série de trabalho, com o treino a que pertence. */
data class SerieDeTreino(
    val sessionId: String,
    val exerciseId: String,
    val weightKg: Double,
    val reps: Int,
)

/**
 * Que treinos bateram algum recorde **no dia em que aconteceram**.
 *
 * O quadro de recordes responde a outra pergunta — o melhor de sempre — e por isso não serve
 * aqui: um treino de há um ano pode ter sido o melhor da vida naquele exercício e não ser o
 * melhor de hoje. A estrela do histórico é sobre o momento, e lê-se para a frente.
 *
 * **Uma passagem sobre as séries todas, e não uma consulta por linha da lista.** O melhor por
 * exercício anda à frente com a lista; o treino em que ele melhora é o treino que ganha a
 * estrela. Com duzentos treinos, o outro caminho são duzentas idas à base.
 *
 * Não se guarda nada: é a decisão que a 2.21.0 tomou ao pôr o [PrDetector] a calcular em vez
 * de guardar um recorde — corrigir uma série corrige a estrela no mesmo instante.
 */
object RecordesPorTreino {

    /**
     * [seriesPorOrdemDeTreino] tem de vir ordenada do treino mais antigo para o mais recente:
     * é a ordem que decide o que era recorde na altura, e ordená-la aqui obrigaria a trazer a
     * hora de cada treino só para a voltar a comparar.
     */
    fun comRecorde(seriesPorOrdemDeTreino: List<SerieDeTreino>): Set<String> {
        val melhor = mutableMapOf<String, ExercisePr>()
        val comRecorde = mutableSetOf<String>()

        for ((sessionId, doTreino) in seriesPorOrdemDeTreino.groupBy { it.sessionId }) {
            // O `groupBy` junta as séries do mesmo exercício, e é isso que impede a segunda
            // série de um exercício de bater o recorde que a primeira acabou de pôr: o treino
            // é comparado inteiro, de uma vez, contra o melhor que havia antes dele.
            for ((exerciseId, series) in doTreino.groupBy { it.exerciseId }) {
                val entradas = series.map { SetEntry(it.weightKg, it.reps) }
                if (PrDetector.detect(melhor[exerciseId], entradas).any) comRecorde += sessionId
                val agora = PrDetector.best(entradas) ?: continue
                melhor[exerciseId] = juntar(melhor[exerciseId], agora)
            }
        }
        return comRecorde
    }

    private fun juntar(anterior: ExercisePr?, agora: ExercisePr): ExercisePr {
        if (anterior == null) return agora
        return ExercisePr(
            // `maxOf` com nulos daria nulo: o melhor de dois, quando um deles não existe, é
            // o outro — e não «não há».
            bestOneRm = listOfNotNull(anterior.bestOneRm, agora.bestOneRm).maxOrNull(),
            bestWeightReps = maxOf(anterior.bestWeightReps, agora.bestWeightReps),
        )
    }
}

fun Double.round1(): Double = (this * 10).roundToInt() / 10.0

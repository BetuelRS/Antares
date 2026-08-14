package pt.antares.app.core.calc

import pt.antares.app.core.model.Sex
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Corrige o gasto energético a partir do que aconteceu de facto: se a pessoa comeu X e
 * o peso mexeu Y, o gasto real deduz-se dos dois. A fórmula de basal é um ponto de
 * partida por estatura e idade; isto é medição.
 *
 * Corre sem rede e sem AI — o `AdaptiveTargetsOfflineTest` falha se alguma chamada de
 * rede voltar a este caminho.
 */
object AdaptiveTdee {

    // Equivalente energético de um quilo de gordura corporal. É a ponte entre peso
    // perdido e calorias, e o [NutritionCalc] usa a mesma constante no sentido inverso.
    const val KCAL_PER_KG = 7700.0

    // Só 30% da observação entra de cada vez. Uma semana com uma gastroenterite ou uma
    // festa não deve reescrever o gasto de quem tem meses de histórico.
    const val SMOOTHING = 0.3

    // Teto do que a meta pode mudar numa semana. Mesmo com dados bons, saltos maiores
    // do que isto fazem a pessoa desistir do plano.
    const val MAX_WEEKLY_CHANGE_KCAL = 200

    // Vetos: sem estes o cálculo devolve sempre um número, e um número errado com ar de
    // medição é pior do que não propor nada.
    const val MIN_LOGGED_DAYS = 5

    const val MIN_WEIGH_INS = 2

    // Acima de 1,5 kg numa semana é água, roupa ou balança diferente, não gordura.
    const val MAX_PLAUSIBLE_WEEKLY_KG = 1.5

    enum class Veto {
        FEW_LOGGED_DAYS,
        FEW_WEIGH_INS,
        IMPLAUSIBLE_WEIGHT_CHANGE,

        LIKELY_METABOLIC_ADAPTATION,
    }

    // Três semanas paradas seguidas. Uma ou duas ainda cabem no ruído normal da balança.
    const val PLATEAU_WEEKS = 3

    // Seis dias em sete. Quem regista quase todos os dias e não perde peso está mesmo
    // num plateau; quem regista quatro provavelmente come mais do que aponta.
    const val FAITHFUL_LOGGING_DAYS = 6

    /**
     * As duas explicações possíveis para um plateau. A app não escolhe pela pessoa —
     * mostra qual delas os dados suportam, porque a resposta é oposta em cada caso:
     * numa come-se mais, na outra regista-se melhor.
     */
    enum class Assessment {

        METABOLIC_ADAPTATION,

        LIKELY_UNDER_LOGGING,

        UNCLEAR,
    }

    fun assessPlateau(consecutiveStallWeeks: Int, loggedDays: Int): Assessment = when {
        consecutiveStallWeeks < PLATEAU_WEEKS -> Assessment.UNCLEAR
        loggedDays >= FAITHFUL_LOGGING_DAYS -> Assessment.METABOLIC_ADAPTATION
        else -> Assessment.LIKELY_UNDER_LOGGING
    }

    data class WeekInput(

        // Média só dos dias registados, não dos sete: dividir por sete faria os dias em
        // branco parecerem jejum e inventava um défice que não existiu.
        val avgIntakeKcal: Double,

        val loggedDays: Int,

        // Diferença da tendência suavizada, não de duas pesagens — ver [WeightTrend].
        val weightTrendDeltaKg: Double,
        val weighIns: Int,

        val currentTdee: Double,

        // O ritmo desejado entra como parcela, não como alvo: o que se corrige é o gasto,
        // e a intenção da pessoa fica intacta por cima dele.
        val goalRateKcal: Int,
        val sex: Sex,

        // Nulo quando não há perfil suficiente para o basal; nesse caso só o chão
        // absoluto do sexo protege a meta.
        val bmr: Double? = null,

        val consecutiveStallWeeks: Int = 0,
    )

    data class Proposal(
        val newTdee: Int,
        val newTargetKcal: Int,
        val previousTargetKcal: Int,

        // O que a semana sozinha diria, antes da suavização. Vai para o ecrã para a
        // proposta ser verificável em vez de sair de uma caixa fechada.
        val observedTdee: Int,
        val clamped: Boolean,
        val flooredToSafety: Boolean,
    ) {
        val deltaKcal: Int get() = newTargetKcal - previousTargetKcal

        // Uma proposta de zero calorias tem de ser silenciada: pedir confirmação para não
        // mudar nada ensina a pessoa a ignorar o pedido quando ele contar.
        val isMeaningful: Boolean get() = deltaKcal != 0
    }

    sealed interface Result {
        data class Propose(val proposal: Proposal) : Result
        data class Skip(val reason: Veto) : Result
    }

    /**
     * Decide se há proposta esta semana e, havendo, qual. Nunca aplica nada: quem aceita
     * é a pessoa, no ecrã.
     */
    fun evaluate(input: WeekInput): Result {

        if (input.loggedDays < MIN_LOGGED_DAYS) return Result.Skip(Veto.FEW_LOGGED_DAYS)
        if (input.weighIns < MIN_WEIGH_INS) return Result.Skip(Veto.FEW_WEIGH_INS)
        if (abs(input.weightTrendDeltaKg) > MAX_PLAUSIBLE_WEEKLY_KG) {
            return Result.Skip(Veto.IMPLAUSIBLE_WEIGHT_CHANGE)
        }

        // Num plateau em défice a conta cá abaixo conclui que o gasto é baixo e manda comer
        // ainda menos — a espiral errada. Aqui pára-se e o ecrã explica as duas hipóteses.
        if (input.goalRateKcal < 0 && input.consecutiveStallWeeks >= PLATEAU_WEEKS) {
            return Result.Skip(Veto.LIKELY_METABOLIC_ADAPTATION)
        }

        // Conservação de energia: o que se comeu menos o que o peso levou. Perder peso dá
        // delta negativo, que soma ao consumo e revela um gasto maior do que a ingestão.
        val observed = input.avgIntakeKcal - (input.weightTrendDeltaKg * KCAL_PER_KG) / 7.0

        val smoothed = (1 - SMOOTHING) * input.currentTdee + SMOOTHING * observed

        val previousTarget = (input.currentTdee + input.goalRateKcal).roundToInt()
        var target = (smoothed + input.goalRateKcal).roundToInt()

        var clamped = false
        val delta = target - previousTarget
        if (abs(delta) > MAX_WEEKLY_CHANGE_KCAL) {
            target = previousTarget + MAX_WEEKLY_CHANGE_KCAL * (if (delta > 0) 1 else -1)
            clamped = true
        }

        // Os mesmos dois chãos das metas fixas. Sem eles, semanas seguidas de correção para
        // baixo levariam a meta abaixo do basal sem que nada travasse.
        val absoluteFloor = when (input.sex) {
            Sex.MALE -> NutritionCalc.FLOOR_MALE
            Sex.FEMALE -> NutritionCalc.FLOOR_FEMALE
        }
        val bmrFloor = input.bmr?.let { (it * NutritionCalc.BMR_FLOOR_FRACTION).roundToInt() } ?: 0
        val floor = maxOf(absoluteFloor, bmrFloor)
        var floored = false
        if (target < floor) {
            target = floor
            floored = true
        }

        return Result.Propose(
            Proposal(
                // O gasto guardado sai da meta já travada, e não do valor suavizado: senão a
                // próxima semana partiria de um número que nunca foi aplicado.
                newTdee = (target - input.goalRateKcal),
                newTargetKcal = target,
                previousTargetKcal = previousTarget,
                observedTdee = observed.roundToInt(),
                clamped = clamped,
                flooredToSafety = floored,
            ),
        )
    }
}

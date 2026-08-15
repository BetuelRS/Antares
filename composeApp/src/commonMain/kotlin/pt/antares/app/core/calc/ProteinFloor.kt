package pt.antares.app.core.calc

/**
 * Quanta proteína por quilo de massa magra é o mínimo, para quem está em défice.
 *
 * O valor era 1,8 para toda a gente. A revisão de Helms et al. (2014, *IJSNEM*) pede **2,3 a
 * 3,1 g/kg de massa magra** a quem treina força em restrição, e diz que sobe com duas coisas:
 * a profundidade do défice e a magreza de quem o faz. Quanto menos gordura há para queimar,
 * mais o corpo vai buscar músculo.
 *
 * A app sabe as duas coisas — tem o histórico de treinos e tem o défice em kcal contra o
 * gasto. Ficar num número fixo era desperdiçar informação que já está lá.
 *
 * **Quem não treina força não muda.** O intervalo de Helms é sobre pessoas que treinam, e
 * estendê-lo a quem não treina era aplicar um estudo fora do que ele estudou.
 */
object ProteinFloor {

    /** Sem treino de força. É o valor que a app sempre usou. */
    const val UNTRAINED_DEFICIT = 1.8

    /** Treina força, défice leve. O fundo do intervalo de Helms. */
    const val TRAINED_LIGHT_DEFICIT = 2.3

    /**
     * Treina força, défice profundo. Helms vai até 3,1, e a app fica em 2,8: o topo do
     * intervalo é para preparação de competição com prazo, que não é o caso de quem usa
     * uma app de nutrição sem acompanhamento.
     */
    const val TRAINED_DEEP_DEFICIT = 2.8

    // O défice medido contra o gasto, e não em kcal absolutas: 600 kcal são um terço do dia
    // de quem gasta 1800 e um quinto do de quem gasta 3000.
    const val LIGHT_DEFICIT_FRACTION = 0.10
    const val DEEP_DEFICIT_FRACTION = 0.25

    /**
     * O chão por quilo de massa magra. `deficitFraction` é a fração do gasto que o défice
     * representa; entre o leve e o profundo interpola-se, para o alvo não dar um salto de
     * meio grama por quilo quando o ritmo muda um bocadinho.
     */
    fun perKgLean(treinaForca: Boolean, deficitFraction: Double): Double {
        if (!treinaForca) return UNTRAINED_DEFICIT

        val posicao = (deficitFraction - LIGHT_DEFICIT_FRACTION) /
            (DEEP_DEFICIT_FRACTION - LIGHT_DEFICIT_FRACTION)
        val entre = posicao.coerceIn(0.0, 1.0)
        return TRAINED_LIGHT_DEFICIT + entre * (TRAINED_DEEP_DEFICIT - TRAINED_LIGHT_DEFICIT)
    }

    /**
     * Quantas semanas de histórico se olham, e quantos treinos acabados dentro delas contam
     * como treinar força.
     *
     * Seis em quatro semanas é uma vez e meia por semana em média. Menos do que isto não é
     * um hábito de treino — é ter ido ao ginásio —, e o intervalo de Helms fala de pessoas
     * treinadas. A média perdoa uma semana falhada, que é o que acontece na vida real.
     */
    const val TRAINED_WINDOW_WEEKS = 4
    const val TRAINED_MIN_SESSIONS = 6

    fun treinaForca(sessoesAcabadas: Int): Boolean = sessoesAcabadas >= TRAINED_MIN_SESSIONS

    /**
     * A fração do gasto que um défice representa. Sai zero quando não há défice ou quando o
     * gasto ainda não é conhecido — e zero cai no extremo leve, que é o lado seguro de errar.
     */
    fun deficitFraction(rateKcal: Int, tdee: Double): Double {
        if (rateKcal >= 0 || tdee <= 0.0) return 0.0
        return -rateKcal / tdee
    }
}

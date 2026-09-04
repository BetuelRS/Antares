package pt.antares.app.core.calc

/**
 * Os períodos dos ecrãs de estatísticas, com quantos dias cada um conta.
 *
 * Nasceu no ecrã da nutrição, onde eram dois — dia e semana — enquanto o Progresso já
 * oferecia 30 dias, três meses e um ano. Um micronutriente não se lê num dia: o fígado guarda
 * vitamina A durante meses, e uma cobertura de 40 % num dia pode ser 100 % no mês. Era o
 * período curto a dar o alarme falso.
 *
 * **Vive aqui, e não dentro de um dos dois ecrãs, porque agora são dois.** As estatísticas do
 * treino usam os mesmos quatro chips com os mesmos nomes, e dois enums iguais em pastas
 * diferentes é o começo de dois vocabulários — a app tem a regra de haver **um** de cada
 * coisa, e um período é tão puro como qualquer conta desta pasta.
 */
enum class StatsPeriod(val dias: Int) {
    DAY(1),
    WEEK(7),
    MONTH(30),

    // 365, e não 366: um dia de diferença não muda uma média, e a alternativa era saber em
    // que ano estamos para uma conta que é sempre aproximada.
    YEAR(365),
    ;

    /**
     * Quantas semanas ISO o período abrange, para quem desenha uma série semana a semana.
     *
     * Arredonda para cima: um mês são trinta dias e cai a meio de uma semana, e cortá-la
     * fora deitava fora os treinos dos últimos dias — que são precisamente os mais recentes.
     */
    val semanas: Int get() = (dias + DIAS_POR_SEMANA - 1) / DIAS_POR_SEMANA

    /**
     * Se vale a pena desenhar uma linha para este período.
     *
     * Abaixo de quatro semanas há um ou dois pontos, e dois pontos não são uma tendência —
     * são um segmento de recta que convida a ler uma direcção que ninguém mediu. É a mesma
     * recusa do `GoalProjection`, que não dá data quando projectar seria adivinhar.
     */
    val temSerie: Boolean get() = semanas >= SEMANAS_MINIMAS_PARA_LINHA

    private companion object {
        const val DIAS_POR_SEMANA = 7
        const val SEMANAS_MINIMAS_PARA_LINHA = 4
    }
}

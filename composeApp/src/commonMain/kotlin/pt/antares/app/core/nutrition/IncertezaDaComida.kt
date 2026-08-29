package pt.antares.app.core.nutrition

import kotlin.math.sqrt

/**
 * De quanto é o «cerca de» nas calorias de um dia.
 *
 * A app já propaga o erro-padrão da fita métrica até ao metabolismo basal — ver o
 * [pt.antares.app.core.calc.NavyUncertainty] —, e continuava a tratar a comida como exacta.
 * **A comida é a maior fonte de erro do sistema inteiro.** Um alimento de tabela é a média de
 * umas quantas amostras de um alimento que varia com a época, o solo e a raça; um rótulo tem
 * tolerâncias legais; uma estimativa de fotografia é um palpite.
 *
 * O que aqui está é **declarado, e não medido**. Nenhum destes números saiu de uma medição
 * nossa, e é por isso que cada um traz a razão por que foi escolhido. Escrever «±7 %» sem
 * poder dizer de onde vêm os sete seria trocar uma falsa precisão por outra.
 *
 * O que **não** está aqui é o maior erro de todos: quantos gramas a pessoa comeu mesmo. Esse
 * é dela e a app não o pode estimar — o que pode é não fingir que os seus são zero.
 */
object IncertezaDaComida {

    /**
     * Uma tabela nacional publica a **média** de umas quantas amostras. O mesmo alimento
     * comprado noutro sítio, noutra época, de outra variedade, não é esse.
     *
     * Dez por cento é o extremo baixo do que a literatura de composição de alimentos aponta
     * para a variação entre amostras do mesmo alimento — costuma citar-se 10 a 20 %. Escolhe-
     * -se o menor pela mesma razão que na fita métrica: um intervalo exagerado ensina a
     * ignorá-lo.
     */
    const val TABELA = 0.10

    /**
     * Um rótulo de embalagem tem tolerâncias legais, e são largas: o regulamento europeu
     * aceita **±20 %** nos macronutrientes declarados acima de 10 g por 100 g. Um número no
     * rótulo não é uma medição daquele pacote — é uma declaração dentro de uma banda.
     */
    const val ROTULO = 0.20

    /**
     * Os alimentos escritos à mão, estimados a partir de receitas e não medidos. É o que o
     * `verified = false` já diz nos ecrãs; aqui diz-se em número.
     */
    const val ESTIMADO = 0.25

    /**
     * Uma estimativa por fotografia ou por texto. **Assumido e largo**, e assumidamente sem
     * medição por trás: não temos nada publicado sobre o erro do nosso, e enquanto não
     * tivermos é preferível um intervalo generoso a um intervalo confortável.
     */
    const val ADIVINHADO = 0.30

    /** O erro relativo de um alimento, pela origem dos números dele. */
    fun de(origem: FoodProvenance): Double = when (origem) {
        FoodProvenance.TCA, FoodProvenance.CIQUAL, FoodProvenance.USDA -> TABELA
        FoodProvenance.OFF -> ROTULO
        FoodProvenance.CURATED -> ESTIMADO
        FoodProvenance.USER -> ROTULO
        FoodProvenance.AI, FoodProvenance.UNKNOWN -> ADIVINHADO
    }

    /** Uma parcela do dia: de onde veio, e quantas kcal trouxe. */
    data class Parcela(val foodId: String, val origem: FoodProvenance, val kcal: Double)

    /**
     * Quanto é que cada origem trouxe ao dia, e quanta margem trouxe com ela.
     *
     * **É isto que torna a margem acionável**, e é o argumento do esboço 22: um dia com
     * ±150 kcal não diz o que fazer, mas «120 dessas vêm dos 400 kcal que a AI adivinhou»
     * diz — pesar aquele prato reduz a margem a metade, e nenhum outro gesto a reduz tanto.
     */
    data class Fatia(val origem: FoodProvenance, val kcal: Double, val maisOuMenos: Double)

    /**
     * O que a app pode dizer sobre um dia inteiro.
     *
     * O [fraccaoAdivinhada] é a parte das calorias que veio de estimativas — de fotografia,
     * de texto, ou de alimentos escritos à mão. É a diferença entre um dia de que se pode
     * falar e um dia em que se andou a adivinhar, e nenhum intervalo sozinho a mostra.
     */
    data class Dia(
        val kcal: Double,
        val maisOuMenos: Double,
        val fraccaoAdivinhada: Double,

        /** Da que mais margem traz para a que menos traz. Vazia num dia sem registos. */
        val porOrigem: List<Fatia> = emptyList(),
    ) {

        val percentagem: Double get() = if (kcal > 0) maisOuMenos / kcal else 0.0

        /**
         * Verdadeiro quando uma diferença é menor do que o «mais ou menos» do próprio dia.
         *
         * É a frase mais honesta que a app pode dizer sobre um único dia: **um défice mais
         * pequeno do que a incerteza da medição não é um défice observado.** Um dia não
         * chega; a tendência da semana chega, porque os erros independentes de sete dias
         * crescem com a raiz de sete e a soma cresce com sete.
         */
        fun menorDoQueOErro(diferencaKcal: Double): Boolean =
            maisOuMenos > 0 && kotlin.math.abs(diferencaKcal) < maisOuMenos
    }

    /**
     * Junta as parcelas do dia num intervalo só.
     *
     * **Dentro do mesmo alimento os erros somam-se; entre alimentos diferentes somam-se em
     * quadratura.** Não é subtileza: duas doses do mesmo arroz partilham o mesmo desvio da
     * mesma tabela — se ela está 10 % alta, está 10 % alta nas duas —, e tratá-las como
     * independentes encolhia o intervalo por um factor de raiz de dois sem razão nenhuma.
     * Alimentos diferentes vêm de análises diferentes, e aí a independência é defensável.
     */
    fun doDia(parcelas: List<Parcela>): Dia {
        val kcal = parcelas.sumOf { it.kcal }
        if (kcal <= 0.0) return Dia(kcal = 0.0, maisOuMenos = 0.0, fraccaoAdivinhada = 0.0)

        val porAlimento = parcelas.groupBy { it.foodId }
        val variancia = porAlimento.values.sumOf { doMesmoAlimento ->
            val somaDoAlimento = doMesmoAlimento.sumOf { it.kcal }
            val erro = somaDoAlimento * de(doMesmoAlimento.first().origem)
            erro * erro
        }

        val adivinhadas = parcelas
            .filter { de(it.origem) >= ESTIMADO }
            .sumOf { it.kcal }

        return Dia(
            kcal = kcal,
            maisOuMenos = sqrt(variancia),
            fraccaoAdivinhada = adivinhadas / kcal,
            porOrigem = porOrigem(porAlimento),
        )
    }

    /**
     * A mesma quadratura, um degrau acima: dentro da origem cada alimento continua a ser
     * independente dos outros, e é por isso que somar as fatias em quadratura devolve
     * exactamente a margem do dia — o `IncertezaDaComidaTest` cobra-o.
     *
     * Fazê-lo de outra maneira — somar as margens a direito dentro da origem — daria um
     * total maior do que o que está escrito por cima da lista, e duas contas do mesmo facto
     * no mesmo ecrã é o defeito a que a 2.6.0 dedicou uma versão inteira noutro sítio.
     */
    private fun porOrigem(porAlimento: Map<String, List<Parcela>>): List<Fatia> = porAlimento.values
        .groupBy { doMesmoAlimento -> doMesmoAlimento.first().origem }
        .map { (origem, alimentos) ->
            val variancia = alimentos.sumOf { doMesmoAlimento ->
                val erro = doMesmoAlimento.sumOf { it.kcal } * de(origem)
                erro * erro
            }
            Fatia(
                origem = origem,
                kcal = alimentos.sumOf { doMesmoAlimento -> doMesmoAlimento.sumOf { it.kcal } },
                maisOuMenos = sqrt(variancia),
            )
        }
        .sortedByDescending { it.maisOuMenos }
}

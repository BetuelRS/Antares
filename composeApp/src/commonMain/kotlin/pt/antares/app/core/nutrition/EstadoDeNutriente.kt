package pt.antares.app.core.nutrition

/**
 * O que se sabe sobre um nutriente quando não há um número.
 *
 * Até à v28 havia uma ausência só. Um zero medido, um «procurámos e há menos do que
 * conseguimos medir», um «há vestígios» e um «ninguém analisou isto» acabavam os quatro como
 * a mesma coisa: a chave não existia no mapa, e o ecrã não dizia nada.
 *
 * São coisas diferentes, e a diferença importa a quem lê. Um alimento com **vestígios** de
 * glúten não é um alimento sem glúten. Um selénio **abaixo do limite de deteção** foi
 * procurado — o que é uma informação — e um selénio ausente não foi.
 *
 * **Os estados são três, e não seis**, porque são três os que as fontes distinguem. O EuroFIR
 * define também «não se aplica» e «assumido zero», e nenhuma das três fontes da app os usa:
 * declará-los era ter estados que nunca acontecem, como os cinco nutrientes que a 2.6.0
 * deixou de fora por não existirem em fonte nenhuma.
 *
 * **Nenhum destes conta para o total do dia.** Somar um vestígio obriga a escolher um número
 * — zero, metade do limite, o limite — e qualquer escolha é aritmética sobre uma coisa que
 * ninguém mediu. Ficam de fora da soma, e o ecrã diz que ficaram.
 */
sealed interface EstadoDeNutriente {

    /**
     * Procurou-se e não se achou o suficiente para medir. O [limite] é o mínimo que o método
     * conseguia detectar — «menos de 0,1 mg» — e é ele que a fonte publica.
     */
    data class AbaixoDoLimite(val limite: Double) : EstadoDeNutriente

    /** Há, e é pouco de mais para pôr um número em cima. */
    data object Vestigios : EstadoDeNutriente

    companion object {

        /** O que se escreve no catálogo para dizer «vestígios». */
        const val MARCA_DE_VESTIGIOS = "vestigios"

        /** O que abre um «abaixo do limite»: `<0.1`. */
        const val MARCA_DE_LIMITE = '<'

        /**
         * Lê um estado escrito no catálogo, ou devolve nulo se aquilo não for um estado.
         *
         * Nulo aqui não é erro: o caso comum é um número, e um número não é um estado. Quem
         * chama isto já tentou ler um número primeiro.
         */
        fun de(texto: String): EstadoDeNutriente? = when {
            texto == MARCA_DE_VESTIGIOS -> Vestigios
            texto.firstOrNull() == MARCA_DE_LIMITE ->
                texto.drop(1).toDoubleOrNull()?.takeIf { it > 0.0 }?.let { AbaixoDoLimite(it) }
            else -> null
        }

        fun escrever(estado: EstadoDeNutriente): String = when (estado) {
            is Vestigios -> MARCA_DE_VESTIGIOS
            is AbaixoDoLimite -> "$MARCA_DE_LIMITE${estado.limite}"
        }
    }
}

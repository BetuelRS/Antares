package pt.antares.app.feature.templates

import pt.antares.app.core.database.entities.MealTemplateEntity
import pt.antares.app.core.database.entities.MealTemplateItemEntity
import kotlin.math.roundToInt

/**
 * Uma refeição guardada, vista antes de entrar no diário.
 *
 * Até à 2.18.0 não havia isto: um toque na linha escrevia os registos e fechava o ecrã, e
 * saber o que tinha entrado obrigava a ir ao diário ver. Uma refeição guardada é uma cópia
 * congelada de um dia que pode ter meses — o que lá está deixou de ser óbvio muito antes de
 * alguém lhe voltar a tocar.
 */
data class PreVisualizacaoDeModelo(
    val modelo: MealTemplateEntity,
    val itens: List<MealTemplateItemEntity>,

    /**
     * Quantas vezes a refeição.
     *
     * **Era um campo de texto, e passa a ser um dos valores de [ESCALAS].** O esboço da área
     * 05 desenha quatro chips e o campo livre foi invenção minha: obrigava a escrever para
     * fazer o que quase sempre se quer — metade, o mesmo, ou o dobro —, e trazia atrás um
     * filtro de algarismos, um limite de caracteres e um estado que aceitava o vazio.
     *
     * Fora da lista não entra nada. Um valor que não esteja nas escalas não tem chip que o
     * ponha lá, e o estado deixa de poder ser inválido.
     */
    val multiplicador: Double = 1.0,
) {
    /** As calorias que vão mesmo entrar, já com o multiplicador. */
    val kcal: Int get() = (itens.sumOf { it.kcalSnapshot } * multiplicador).roundToInt()

    /** As gramas de um item, já multiplicadas — é o que a linha da lista mostra. */
    fun gramasDe(item: MealTemplateItemEntity): Double = item.quantityGrams * multiplicador

    fun kcalDe(item: MealTemplateItemEntity): Int = (item.kcalSnapshot * multiplicador).roundToInt()

    /**
     * Escolhe uma escala. Um valor de fora da lista fica de fora — não há caminho na
     * interface que lá chegue, e aceitá-lo aqui era deixar a porta aberta a um zero que
     * escreveria registos de zero calorias.
     */
    fun comEscala(escala: Double): PreVisualizacaoDeModelo =
        if (escala in ESCALAS) copy(multiplicador = escala) else this

    companion object {

        /**
         * As quatro do esboço, por esta ordem.
         *
         * Meia refeição, a refeição, uma vez e meia, duas. Cobre o que se faz — comi metade,
         * comi a dobrar — sem pedir que se escreva um número para o caso normal, que é ×1.
         */
        val ESCALAS = listOf(0.5, 1.0, 1.5, 2.0)
    }
}

/**
 * O que se lê num chip: `×1`, `×1,5`.
 *
 * O inteiro perde a casa decimal — `×1,0` num chip lê-se como uma precisão que não existe.
 * A vírgula é a mesma decisão de idioma do resto da app, e entra por parâmetro para esta
 * função poder ser testada sem composição nenhuma à volta.
 */
fun rotuloDaEscala(escala: Double, virgula: Boolean): String {
    val texto = escala.toString().removeSuffix(".0")
    return "×" + if (virgula) texto.replace('.', ',') else texto
}

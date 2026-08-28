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
     * Quantas vezes a refeição, como está escrito no campo.
     *
     * Vive como texto e não como número pela mesma razão do campo de gramas da folha da AI:
     * quem apaga «1» para escrever «0,5» passa por um campo vazio, e um estado que recuse o
     * vazio é um campo que não se consegue limpar.
     */
    val multiplicadorTexto: String = "1",
) {
    /**
     * O número que vale, ou **1 quando o texto ainda não é um número**.
     *
     * Nunca zero nem negativo: aplicar zero vezes uma refeição escreve sete linhas de zero
     * calorias, que é pior do que não escrever nada — ficam no diário a somar nada e a
     * ocupar a lista.
     */
    val multiplicador: Double
        get() = multiplicadorTexto.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0

    /** As calorias que vão mesmo entrar, já com o multiplicador. */
    val kcal: Int get() = (itens.sumOf { it.kcalSnapshot } * multiplicador).roundToInt()

    /** As gramas de um item, já multiplicadas — é o que a linha da lista mostra. */
    fun gramasDe(item: MealTemplateItemEntity): Double = item.quantityGrams * multiplicador

    fun kcalDe(item: MealTemplateItemEntity): Int = (item.kcalSnapshot * multiplicador).roundToInt()

    fun comTexto(texto: String): PreVisualizacaoDeModelo =
        copy(multiplicadorTexto = apenasNumero(texto).take(MAX_CARACTERES))

    private companion object {

        // Chega para «0,25» e para «10». Uma refeição multiplicada por mil não é uma
        // refeição; é um dedo preso no teclado.
        const val MAX_CARACTERES = 4
    }
}

/**
 * Algarismos e, quando muito, um separador decimal.
 *
 * O teclado deste campo já é o dos números, mas um teclado de hardware escreve o que quiser
 * — foi assim que apareceu o mesmo problema no campo de gramas da folha da AI, na 2.17.0.
 */
private fun apenasNumero(texto: String): String {
    val limpo = StringBuilder()
    var jaTemSeparador = false
    for (c in texto) {
        when {
            c.isDigit() -> limpo.append(c)
            (c == ',' || c == '.') && !jaTemSeparador -> {
                jaTemSeparador = true
                limpo.append(c)
            }
        }
    }
    return limpo.toString()
}

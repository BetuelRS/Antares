package pt.antares.app.feature.fooddata

import pt.antares.app.feature.recipe.RecipeSummary
import pt.antares.app.feature.templates.ModeloComResumo

/**
 * Uma refeição que já está montada, seja qual for o caminho por onde foi montada.
 *
 * **O esboço da área 05 pede «uma lista só», e este tipo é o que a torna possível.** Havia
 * dois conceitos — o modelo, que congela um dia do diário, e a receita, que soma
 * ingredientes — com duas listas, dois separadores e nenhum ponto de encontro. A pergunta
 * que se faz aos dois é a mesma: «o que é que eu já montei?». A diferença é como se
 * construiu, e isso não vale uma escolha antes de olhar para a lista.
 *
 * A distinção não desaparece: passa da arrumação para **a linha**, que diz a origem em
 * palavras. É a troca que o estudo propõe — o utilizador deixa de a ter de saber para
 * navegar, e continua a poder lê-la.
 */
sealed interface RefeicaoGuardada {

    /** Único na lista inteira: as duas origens têm identificadores de tabelas diferentes. */
    val chave: String

    val nome: String

    /** Guardada do diário: uma cópia congelada de um dia. */
    data class DoDiario(val resumo: ModeloComResumo) : RefeicaoGuardada {
        override val chave get() = "modelo-${resumo.modelo.id}"
        override val nome get() = resumo.modelo.name
    }

    /** Montada de ingredientes, com peso — o que a app chamava «receita». */
    data class DeIngredientes(val resumo: RecipeSummary) : RefeicaoGuardada {
        override val chave get() = "receita-${resumo.recipe.id}"
        override val nome get() = resumo.recipe.name
    }
}

/**
 * As duas origens numa lista só, por ordem de nome.
 *
 * **Por nome e não por origem**, que é o ponto: agrupar por origem era voltar às duas
 * secções com outro aspecto. Quem procura uma refeição procura-a pelo nome dela, e é a
 * única ordem em que a lista se percorre sem saber primeiro o que se está a procurar.
 *
 * A comparação ignora maiúsculas — «almoço» e «Almoço» ficarem em pontas opostas da lista
 * seria o alfabeto a mandar mais do que a leitura.
 */
fun juntarRefeicoes(
    modelos: List<ModeloComResumo>,
    receitas: List<RecipeSummary>,
): List<RefeicaoGuardada> =
    (modelos.map(RefeicaoGuardada::DoDiario) + receitas.map(RefeicaoGuardada::DeIngredientes))
        .sortedBy { it.nome.lowercase() }

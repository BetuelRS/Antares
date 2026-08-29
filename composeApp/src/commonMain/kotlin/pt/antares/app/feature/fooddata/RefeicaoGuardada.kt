package pt.antares.app.feature.fooddata

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.common_grams_short
import pt.antares.app.generated.resources.common_kcal
import pt.antares.app.generated.resources.modelo_itens
import pt.antares.app.generated.resources.refeicao_do_diario
import pt.antares.app.generated.resources.refeicao_doses
import pt.antares.app.generated.resources.refeicao_ingredientes
import pt.antares.app.generated.resources.refeicao_kcal_dose
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

/**
 * O que a linha diz sobre uma refeição sem a abrir — **incluindo de onde ela veio**.
 *
 * Com as duas origens na mesma lista, a origem passa a ser conteúdo da linha e não o sítio
 * onde ela está. A guardada do diário di-lo por extenso; a montada de ingredientes di-lo
 * pelas palavras que usa, «ingredientes» e «doses», que a outra nunca tem.
 */
@Composable
internal fun subtituloDaRefeicao(refeicao: RefeicaoGuardada): String = when (refeicao) {
    is RefeicaoGuardada.DoDiario -> {
        val r = refeicao.resumo
        pluralStringResource(Res.plurals.modelo_itens, r.itens, r.itens) +
            " · ${r.kcal} ${stringResource(Res.string.common_kcal)}" +
            " · ${stringResource(Res.string.refeicao_do_diario)}"
    }

    is RefeicaoGuardada.DeIngredientes -> {
        val r = refeicao.resumo
        val doses = r.recipe.servings?.takeIf { it > 0 }
        val ingredientes = pluralStringResource(
            Res.plurals.refeicao_ingredientes,
            r.ingredientCount,
            r.ingredientCount,
        )
        // Com doses definidas, as calorias que interessam são as de uma dose: é a
        // quantidade que se regista. Sem elas, o por-100-g é o único número honesto.
        if (doses != null) {
            ingredientes +
                " · ${stringResource(Res.string.refeicao_kcal_dose, r.nutrition.totalKcal / doses)}" +
                " · ${pluralStringResource(Res.plurals.refeicao_doses, doses, doses)}"
        } else {
            ingredientes +
                " · ${r.nutrition.kcalPer100} ${stringResource(Res.string.common_kcal)}" +
                " / 100 ${stringResource(Res.string.common_grams_short)}"
        }
    }
}

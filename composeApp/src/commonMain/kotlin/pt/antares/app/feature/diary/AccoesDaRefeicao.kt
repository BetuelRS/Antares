package pt.antares.app.feature.diary

import pt.antares.app.core.model.MealSlot

/**
 * O que o menu de uma refeição do diário sabe fazer.
 *
 * Vieram para um sítio só quando a 2.18.0 juntou a quinta: cinco lambdas soltas, quatro
 * delas `() -> Unit`, trocam-se umas pelas outras sem o compilador dizer nada — e um
 * «limpar» ligado ao «guardar» é um dia apagado por engano.
 */
internal data class AccoesDaRefeicao(
    val onSaveAsTemplate: () -> Unit,

    // O contrário de guardar, e no mesmo menu: é aqui que se olha para uma refeição vazia e
    // se pensa «hoje é o mesmo de sempre».
    val onApplyTemplate: () -> Unit,
    val onCopyFromDay: () -> Unit,
    val onMoveMeal: (MealSlot) -> Unit,
    val onClearMeal: () -> Unit,
)

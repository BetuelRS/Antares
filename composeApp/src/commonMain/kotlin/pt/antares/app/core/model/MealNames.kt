package pt.antares.app.core.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.meal_breakfast
import pt.antares.app.generated.resources.meal_dinner
import pt.antares.app.generated.resources.meal_lunch
import pt.antares.app.generated.resources.meal_snack

/**
 * Nomes de refeição escolhidos pela pessoa. Só o nome muda: o [MealSlot] por trás continua
 * o mesmo, e é por isso que chamar "ceia" ao jantar não mexe em nenhum registo antigo.
 */
data class MealNames(val overrides: Map<MealSlot, String> = emptyMap()) {
    fun override(slot: MealSlot): String? = overrides[slot]?.takeIf { it.isNotBlank() }
}

// Passa por composição local em vez de por parâmetro porque o nome da refeição aparece em
// dezenas de ecrãs, e enfiá-lo em cada assinatura só para o levar mais fundo não ajuda.
val LocalMealNames = compositionLocalOf { MealNames() }

@Composable
fun mealSlotLabel(slot: MealSlot): String =
    LocalMealNames.current.override(slot) ?: mealSlotLabelDefault(slot)

@Composable
fun mealSlotLabelDefault(slot: MealSlot): String = stringResource(
    when (slot) {
        MealSlot.BREAKFAST -> Res.string.meal_breakfast
        MealSlot.LUNCH -> Res.string.meal_lunch
        MealSlot.DINNER -> Res.string.meal_dinner
        MealSlot.SNACK -> Res.string.meal_snack
    },
)

/** Variante para quem só tem o nome da constante — vindo de JSON ou de uma consulta. */
@Composable
fun mealSlotLabel(slotName: String?): String {
    // Texto vazio para o desconhecido: uma etiqueta em branco desaparece do ecrã sem o
    // partir, ao contrário de uma exceção.
    val slot = MealSlot.entries.firstOrNull { it.name == slotName } ?: return ""
    return mealSlotLabel(slot)
}

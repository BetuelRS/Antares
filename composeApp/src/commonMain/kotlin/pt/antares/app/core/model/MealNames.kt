package pt.antares.app.core.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.meal_breakfast
import pt.antares.app.generated.resources.meal_dinner
import pt.antares.app.generated.resources.meal_lunch
import pt.antares.app.generated.resources.meal_snack

data class MealNames(val overrides: Map<MealSlot, String> = emptyMap()) {
    fun override(slot: MealSlot): String? = overrides[slot]?.takeIf { it.isNotBlank() }
}

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

@Composable
fun mealSlotLabel(slotName: String?): String {
    val slot = MealSlot.entries.firstOrNull { it.name == slotName } ?: return ""
    return mealSlotLabel(slot)
}

package pt.antares.app.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.calc.GoalChange
import pt.antares.app.core.calc.GoalChangeReason
import pt.antares.app.core.designsystem.bodyWeightWithUnit
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.feature.profile.data.GoalMigrationRepository
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.abs

class GoalChangeViewModel(
    private val repository: GoalMigrationRepository,
) : ViewModel() {

    private val _change = MutableStateFlow<GoalChange?>(null)
    val change: StateFlow<GoalChange?> = _change

    init {
        viewModelScope.launch { _change.value = repository.pendingGoalChange() }
    }

    fun acknowledge() {
        viewModelScope.launch {
            repository.acknowledge()
            _change.value = null
        }
    }
}

@Composable
fun GoalChangeCard(change: GoalChange, onDismiss: () -> Unit) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.goal_change_title),
            style = MaterialTheme.typography.titleMedium,
        )

        if (change.deltaKcal != 0) {
            Text(
                stringResource(
                    if (change.deltaKcal < 0) {
                        Res.string.goal_change_down
                    } else {
                        Res.string.goal_change_up
                    },
                    change.oldKcal,
                    change.newKcal,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        for (reason in change.reasons) {
            Text(
                "· ${stringResource(reason.explanation())}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.goal_change_ack))
            }
        }
    }
}

@Composable
fun WeightRecalcNotice(oldKcal: Int, newKcal: Int, deltaWeightKg: Double) {
    if (oldKcal == newKcal) return
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(
                if (deltaWeightKg < 0) Res.string.recalc_lost else Res.string.recalc_gained,
                bodyWeightWithUnit(abs(deltaWeightKg), rememberUnitSystem()),
                oldKcal,
                newKcal,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun GoalChangeReason.explanation() = when (this) {
    GoalChangeReason.ACTIVITY_MEANING_CHANGED -> Res.string.goal_change_activity
    GoalChangeReason.BMR_FORMULA_CHANGED -> Res.string.goal_change_formula
    GoalChangeReason.ENERGY_FLOOR_CHANGED -> Res.string.goal_change_floor
    GoalChangeReason.EXERCISE_ADD_BACK_FORCED_ON -> Res.string.goal_change_addback
}

package pt.antares.app.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.calc.DietBreak
import pt.antares.app.core.calc.DietBreakSuggestion
import pt.antares.app.core.calc.WeightTrend
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.GoalRates
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.profile.data.ProfileRepository
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

class DietBreakViewModel(
    private val repository: ProfileRepository,
) : ViewModel() {

    private val _suggestion = MutableStateFlow<DietBreakSuggestion?>(null)
    val suggestion: StateFlow<DietBreakSuggestion?> = _suggestion

    private val _accepted = MutableStateFlow(false)
    val accepted: StateFlow<Boolean> = _accepted

    init {
        viewModelScope.launch {
            val targets = repository.targetsFor(todayEpochDay()) ?: return@launch
            val tdee = targets.energy?.tdee ?: return@launch
            val weights = repository.weightsChronological()
            val stallWeeks = WeightTrend.consecutiveStallWeeks(weights)
            _suggestion.value = DietBreak.suggest(
                currentTdee = tdee,
                consecutiveStallWeeks = stallWeeks,

                loggedDays = repository.loggedDaysPerWeek(stallWeeks),
            )
        }
    }

    fun accept() {
        viewModelScope.launch {
            val profile = repository.profileOnce() ?: return@launch
            repository.saveProfile(
                profile.copy(goalType = GoalType.MAINTAIN, goalRateKcal = GoalRates.MAINTAIN),
            )
            _accepted.value = true
        }
    }
}

@Composable
fun DietBreakScreen(
    onBack: () -> Unit,
    viewModel: DietBreakViewModel = koinViewModel(),
) {
    val suggestion by viewModel.suggestion.collectAsState()
    val accepted by viewModel.accepted.collectAsState()

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.diet_break_title), onBack = onBack) },
    ) { padding ->
        val s = suggestion
        if (s == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(Res.string.diet_break_body, s.maintenanceKcal, s.weeks),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (accepted) {
                Text(
                    stringResource(Res.string.settings_goal_reached_cta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                SecondaryButton(
                    text = stringResource(Res.string.common_back),
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PrimaryButton(
                    text = stringResource(Res.string.diet_break_cta, s.weeks),
                    onClick = viewModel::accept,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                stringResource(Res.string.profile_health_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

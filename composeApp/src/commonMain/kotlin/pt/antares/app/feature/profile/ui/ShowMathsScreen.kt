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
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.calc.TargetBreakdown
import pt.antares.app.core.calc.TargetBreakdownCalc
import pt.antares.app.core.calc.TargetBreakdownText
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.profile.data.ProfileRepository
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

class ShowMathsViewModel(repository: ProfileRepository) : ViewModel() {

    private val _breakdown = MutableStateFlow<TargetBreakdown?>(null)
    val breakdown: StateFlow<TargetBreakdown?> = _breakdown

    init {
        combine(
            repository.observeProfile(),
            repository.observeLatestWeight(),
            repository.observeTargets(),
        ) { profile, weight, targets ->
            if (profile == null || targets == null) {
                null
            } else {
                TargetBreakdownCalc.of(
                    profile = profile,
                    targets = targets,
                    weightKg = weight?.weightKg ?: ProfileRepository.DEFAULT_WEIGHT_KG,
                    todayEpochDay = todayEpochDay(),
                )
            }
        }
            .onEach { _breakdown.value = it }
            .launchIn(viewModelScope)
    }
}

@Composable
fun ShowMathsScreen(
    onBack: () -> Unit,
    viewModel: ShowMathsViewModel = koinViewModel(),
) {
    val breakdown by viewModel.breakdown.collectAsState()

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.show_maths_title), onBack = onBack) },
    ) { padding ->
        val b = breakdown
        if (b == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .larguraDeLeitura()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            for (step in b.steps) {
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    Text(stepText(step), style = MaterialTheme.typography.bodyLarge)
                }
            }
            Text(
                stringResource(Res.string.show_maths_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun stepText(step: TargetBreakdown.Step): String {
    val comma = Locale.current.language == "pt"
    val a = TargetBreakdownText.args(step, comma)
    return when (step.kind) {
        TargetBreakdown.Kind.BMR_FROM_LEAN ->
            stringResource(Res.string.show_maths_lean, a[0], a[1], a[2], a[3])

        TargetBreakdown.Kind.BMR_MIFFLIN ->
            stringResource(Res.string.show_maths_mifflin, a[0], a[1], a[2], a[3], a[4])

        TargetBreakdown.Kind.ACTIVITY ->
            stringResource(Res.string.show_maths_activity, a[0], a[1], a[2])

        TargetBreakdown.Kind.RATE ->
            stringResource(Res.string.show_maths_rate, a[0], a[1], a[2])

        TargetBreakdown.Kind.FLOOR -> stringResource(Res.string.onb_rate_floor_warning)

        TargetBreakdown.Kind.BMR_UNCERTAIN ->
            stringResource(Res.string.show_maths_bmr_uncertain, a[0], a[1])

        TargetBreakdown.Kind.PROTEIN_TRAINED ->
            stringResource(Res.string.show_maths_protein_trained, a[0], a[1], a[2])
    }
}

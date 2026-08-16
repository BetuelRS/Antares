package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.feature.workout.model.WorkoutTaxonomy
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun ExerciseCreateScreen(
    onCreated: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ExerciseCreateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.createdId) { state.createdId?.let(onCreated) }

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.excustom_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .larguraDeLeitura()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            OutlinedTextField(
                value = state.namePt,
                onValueChange = viewModel::setNamePt,
                label = { Text(stringResource(Res.string.excustom_name_pt)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.nameEn,
                onValueChange = viewModel::setNameEn,
                label = { Text(stringResource(Res.string.excustom_name_en)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SelectField(
                label = Res.string.excustom_category,
                selected = state.category,
                options = WorkoutTaxonomy.categories,
                optionLabel = ::categoryLabel,
                onSelect = { it?.let(viewModel::setCategory) },
                allowNone = false,
            )
            SelectField(
                label = Res.string.excustom_primary_muscle,
                selected = state.primaryMuscle,
                options = WorkoutTaxonomy.muscles,
                optionLabel = ::muscleLabel,
                onSelect = viewModel::setPrimaryMuscle,
                allowNone = true,
            )
            SelectField(
                label = Res.string.excustom_equipment,
                selected = state.equipment,
                options = WorkoutTaxonomy.equipment,
                optionLabel = ::equipmentLabel,
                onSelect = viewModel::setEquipment,
                allowNone = true,
            )
            PrimaryButton(
                text = stringResource(Res.string.common_save),
                onClick = viewModel::save,
                enabled = state.valid,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun SelectField(
    label: StringResource,
    selected: String?,
    options: List<String>,
    optionLabel: (String) -> StringResource,
    onSelect: (String?) -> Unit,
    allowNone: Boolean,
) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                (selected?.let { stringResource(optionLabel(it)) } ?: stringResource(label)),
                modifier = Modifier.weight(1f),
            )
            // Decorativo: a seta diz que abre, e o botão já tem o rótulo escrito.
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (allowNone) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.exlib_filter_all)) },
                    onClick = { onSelect(null); open = false },
                )
            }
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(stringResource(optionLabel(opt))) },
                    onClick = { onSelect(opt); open = false },
                )
            }
        }
    }
}

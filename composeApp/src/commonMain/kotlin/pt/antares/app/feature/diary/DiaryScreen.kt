package pt.antares.app.feature.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.calc.DailyBudgetCalc
import pt.antares.app.core.database.entities.ExerciseLogEntity
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import pt.antares.app.core.model.Sex
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.ExerciseOrigin
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.AntaresColors
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.success
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresGhostCard
import pt.antares.app.core.designsystem.components.MacroBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.designsystem.components.StatRing
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.model.mealSlotLabel
import pt.antares.app.core.util.dayShort
import pt.antares.app.core.util.epochDayToLocalDate
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import pt.antares.app.core.util.MINUTES_PER_HOUR
import pt.antares.app.core.util.formatMinuteOfDay
import pt.antares.app.core.calc.Janela
import pt.antares.app.core.util.formatDurationMin

@Composable
fun DiaryScreen(
    onAddFood: (MealSlot, Long, pt.antares.app.feature.fooddata.AddMode) -> Unit,
    onAddExercise: (Long) -> Unit,

    onQuickLog: (MealSlot, Long, pt.antares.app.feature.fooddata.AddMode, String) -> Unit,

    // O dia e a refeição vão com o alimento para o caminho de volta cair no mesmo sítio.
    onOpenFood: (String, MealSlot, Long) -> Unit,
    viewModel: DiaryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val repeatable by viewModel.repeatable.collectAsState()
    val aguaDaComidaMl by viewModel.aguaDaComidaMl.collectAsState()
    val folhas = remember { DiarySheets() }
    DiaryDialogHost(
        folhas = folhas,
        viewModel = viewModel,
        epochDay = state.epochDay,
        logsBySlot = state.logsBySlot,
        onAddFood = onAddFood,
        onQuickLog = onQuickLog,
        onOpenFood = onOpenFood,
    )

    LazyColumn(
        // O diário fica numa coluna só, mesmo num tablet: as refeições são secções, e
        // parti-las por colunas separava o que a pessoa comeu ao almoço do cabeçalho
        // "Almoço". Trava na largura de leitura e fica ao meio.
        modifier = Modifier
            .fillMaxSize()
            .larguraDeLeitura()
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {

        item(key = "day-header") {
            DayHeader(
                isToday = state.isToday,
                epochDay = state.epochDay,
                onPrevious = viewModel::previousDay,
                onNext = viewModel::nextDay,
                onToday = viewModel::goToToday,
            )
        }

        state.quebraDoJejum?.let { quebra ->
            item(key = "jejum-quebrado") { QuebraDoJejumCartao(quebra) }
        }

        item(key = "quick-log") {
            // Num dia que não é hoje, a hora do relógio não diz nada sobre a refeição: quem
            // regista o jantar de ontem de manhã levava-o para o pequeno-almoço. Aí
            // pergunta-se, e o pedido fica à espera de resposta.
            fun registar(mode: pt.antares.app.feature.fooddata.AddMode, q: String) {
                if (state.isToday) {
                    onQuickLog(
                        MealSlot.atHour(pt.antares.app.core.util.currentHour()),
                        state.epochDay,
                        mode,
                        q,
                    )
                } else {
                    folhas.quickLogPendente = QuickLogPendente(mode, q)
                }
            }
            pt.antares.app.feature.fooddata.QuickLogBar(
                onSubmit = { q -> registar(pt.antares.app.feature.fooddata.AddMode.SEARCH, q) },
                onVoice = { q -> registar(pt.antares.app.feature.fooddata.AddMode.DESCRIBE, q) },
                onPhoto = { registar(pt.antares.app.feature.fooddata.AddMode.PHOTO, "") },
                onScan = { registar(pt.antares.app.feature.fooddata.AddMode.SCAN, "") },
            )
        }

        item(key = "day-summary") {
            DaySummaryCard(state)
        }

        if (state.logsBySlot.isEmpty()) {
            item {
                SecondaryButton(
                    text = stringResource(Res.string.diary_copy_yesterday),
                    onClick = viewModel::copyYesterday,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        MealSlot.entries.forEach { slot ->
            mealSection(
                slot = slot,
                logs = state.logsBySlot[slot].orEmpty(),
                sugestao = repeatable[slot],
                viewModel = viewModel,
                folhas = folhas,
            )
        }

        exerciseSection(
            entries = state.exerciseEntries,
            kcal = state.exerciseKcal,
            onAdd = { onAddExercise(state.epochDay) },
            onEdit = { folhas.editExercise = it },
            onDelete = viewModel::deleteExercise,
            onRestore = viewModel::restoreExercise,
        )

        item {
            WaterCard(
                bebidaMl = state.waterMl,
                daComidaMl = aguaDaComidaMl,
                metaMl = state.waterGoalMl,
                onAdd = viewModel::addWater,
            )
        }

        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

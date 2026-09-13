package pt.antares.app.feature.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.designsystem.components.rememberDesfazer
import pt.antares.app.core.model.MealSlot
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

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
    val aguaDaComida by viewModel.aguaDaComida.collectAsState()
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
                onPickDate = { folhas.pickDateOpen = true },
                onToday = { viewModel.goToDay(pt.antares.app.core.util.todayEpochDay()) },
                onCopyDay = { folhas.copyDayOpen = true; viewModel.loadCopyDayCandidates() },
                onSearch = { folhas.searchOpen = true },
            )
        }

        item(key = "week-strip") {
            val diasDaSemana by viewModel.diasDaSemana.collectAsState()
            DiaryWeekStrip(
                epochDay = state.epochDay,
                diasRegistados = diasDaSemana,
                onDiaClick = viewModel::goToDay,
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
            DaySummaryCard(state, aguaDaComida)
        }

        if (state.logsBySlot.isEmpty()) {
            item {
                // A mesma cópia do «Copiar dia inteiro» do menu, com o mesmo desfazer: duas
                // portas para a mesma acção não podem ter redes diferentes.
                val desfazer = rememberDesfazer()
                val mensagem = stringResource(Res.string.diary_copy_day_undo)
                SecondaryButton(
                    text = stringResource(Res.string.diary_copy_yesterday),
                    onClick = {
                        viewModel.copyDayFrom(state.epochDay - 1) { criados ->
                            // Ontem vazio não copia nada, e um desfazer de nada seria mentir.
                            if (criados.isNotEmpty()) {
                                desfazer(mensagem) { viewModel.desfazerCopiaDoDia(criados) }
                            }
                        }
                    },
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
                daComida = aguaDaComida,
                metaMl = state.waterGoalMl,
                onAdd = viewModel::addWater,
            )
        }

        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

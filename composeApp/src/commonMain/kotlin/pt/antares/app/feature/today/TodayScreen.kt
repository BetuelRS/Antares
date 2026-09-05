package pt.antares.app.feature.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt
import pt.antares.app.core.calc.DailyBudgetCalc
import pt.antares.app.core.calc.Targets
import pt.antares.app.core.calc.WeeklyBudget
import pt.antares.app.core.database.entities.FastingSessionEntity
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.nutrition.DailyGap
import pt.antares.app.core.nutrition.Nutrients
import pt.antares.app.core.nutrition.microLabelRes
import pt.antares.app.core.designsystem.virgulaDecimal
import pt.antares.app.core.designsystem.AntaresColors
import pt.antares.app.core.designsystem.cascadeIn
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.feature.backup.AvisoDeCopiaAtrasada
import pt.antares.app.core.designsystem.distanceUnitLabel
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.UnitConversions
import pt.antares.app.core.designsystem.success
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.weightWithUnit
import pt.antares.app.core.designsystem.components.GrelhaDeCartoes
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresHeroCard
import pt.antares.app.feature.coach.CoachTeaserCard
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import pt.antares.app.core.designsystem.components.StatRing
import pt.antares.app.core.designsystem.components.SupernovaCelebration
import pt.antares.app.feature.fasting.data.toSnapshot
import pt.antares.app.feature.fasting.domain.FastingMachine
import pt.antares.app.feature.onboarding.OnboardingStep
import pt.antares.app.feature.running.ui.RunFormat
import pt.antares.app.feature.fasting.ui.FastingFormat
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun TodayScreen(
    destinos: DestinosDoHoje,

    onQuickLog: (pt.antares.app.core.model.MealSlot, Long, pt.antares.app.feature.fooddata.AddMode, String) -> Unit,

    onOpenGap: (String) -> Unit,
    viewModel: TodayViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val workout by viewModel.workout.collectAsState()
    val fasting by viewModel.fasting.collectAsState()
    val lastRun by viewModel.lastRun.collectAsState()
    val steps by viewModel.steps.collectAsState()
    val streak by viewModel.loggingStreak.collectAsState()
    val celebration by viewModel.celebration.collectAsState()
    val weeklyBudget by viewModel.weeklyBudget.collectAsState()
    val dailyGap by viewModel.dailyGap.collectAsState()
    val aguaDaComidaMl by viewModel.aguaDaComidaMl.collectAsState()
    val porResponder by viewModel.porResponder.collectAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) { viewModel.syncHealthConnect() }

    var nowMin by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = Clock.System.now().toEpochMilliseconds()
            nowMin = now
            delay(60_000 - (now % 60_000))
        }
    }

    if (state.loading) {
        LoadingState()
        return
    }
    val targets = state.targets
    if (targets == null) {

        // Sem perfil não há metas para mostrar — e havia uma frase, um `return`, e mais nada.
        // É o defeito concreto 3 da `estudo/areas/01-hoje.md`: um estado raro, e sem saída
        // nenhuma. É também o primeiro ecrã que aparece a quem chega aqui, e por isso o botão
        // vai ao sítio onde as respostas se dão.
        Column(Modifier.fillMaxSize().padding(Spacing.xl), verticalArrangement = Arrangement.Center) {
            Text(stringResource(Res.string.today_no_profile), style = MaterialTheme.typography.bodyLarge)
            PrimaryButton(
                text = stringResource(Res.string.today_no_profile_action),
                onClick = destinos.arranque,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
            )
        }
        return
    }

    celebration?.let { days ->
        LaunchedEffect(days) { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
        SupernovaCelebration(
            title = stringResource(Res.string.celebration_title),
            subtitle = stringResource(Res.string.celebration_streak_days, days),
            onDismiss = viewModel::consumeCelebration,
        )
    }

    GrelhaDeCartoes(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        // A barra de registo rápido fica por cima e a toda a largura: é a primeira coisa que
        // a pessoa toca, e numa coluna de metade do ecrã ficava com meio campo de texto.
        cabecalho = {
            val slot = pt.antares.app.core.model.MealSlot.atHour(pt.antares.app.core.util.currentHour())
            val day = pt.antares.app.core.util.todayEpochDay()
            pt.antares.app.feature.fooddata.QuickLogBar(
                onSubmit = { q -> onQuickLog(slot, day, pt.antares.app.feature.fooddata.AddMode.SEARCH, q) },
                onVoice = { q ->
                    onQuickLog(slot, day, pt.antares.app.feature.fooddata.AddMode.DESCRIBE, q)
                },
                onPhoto = { onQuickLog(slot, day, pt.antares.app.feature.fooddata.AddMode.PHOTO, "") },
                onScan = { onQuickLog(slot, day, pt.antares.app.feature.fooddata.AddMode.SCAN, "") },
            )
        },
    ) {

        // Em primeiro e não em último: o cartão só existe quando a cópia está atrasada, e
        // quando existe é a coisa mais urgente do ecrã — todo o resto se volta a registar,
        // três anos de diário não.
        cartao { AvisoDeCopiaAtrasada() }

        cartao { CoachTeaserCard(onOpen = destinos.treinador) }

        if (streak.current >= 1) {
            cartao { StreakCard(streak = streak) }
        }

        if (porResponder.isNotEmpty()) {
            cartao {
                RespostasEmFaltaCard(
                    passos = porResponder,
                    onAnswer = destinos.perfil,
                    onDismiss = viewModel::naoPerguntar,
                )
            }
        }

        cartao { CartaoDaMeta(targets = targets, state = state, onAddMeal = destinos.refeicao) }

        weeklyBudget?.let { orcamento -> cartao { WeeklyBudgetCard(orcamento) } }

        dailyGap?.let { folga -> cartao { DailyGapCard(folga, onOpenGap) } }

        cartao {
            CartaoDaAgua(state = state, aguaDaComidaMl = aguaDaComidaMl, onAbrirDiario = destinos.refeicao)
        }

        cartao {
            CartaoDoTreino(
                treino = workout,
                unidades = state.unitSystem,
                onAbrir = destinos.treino,
            )
        }

        cartao { CartaoDoJejum(sessao = fasting, agoraMs = nowMin, onAbrir = destinos.jejum) }

        cartao {
            CartaoDaCorrida(
                corrida = lastRun,
                unidades = state.unitSystem,
                onAbrir = destinos.corrida,
            )
        }

        steps?.let { passos -> cartao { CartaoDosPassos(passos) } }

        cartao { CartaoDoPeso(state = state, onRegistarPeso = destinos.peso) }
    }
}

/**
 * O cartão da meta do dia: o anel, o que sobra, os macros e o botão de registar. É o único
 * que mostra o orçamento já com o exercício somado — o `DailyBudgetCalc` é que decide isso,
 * e o cartão limita-se a mostrar o que ele devolve.
 */
@Composable
private fun CartaoDaMeta(targets: Targets, state: TodayState, onAddMeal: () -> Unit) {
    AntaresHeroCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(Res.string.today_target_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Spacing.md))

            val budget = DailyBudgetCalc.compute(
                target = targets.kcal,
                consumed = state.consumed.kcal,
                exercise = state.exerciseKcal,
            )

            StatRing(
                progress = if (budget.budget > 0) budget.consumed.toFloat() / budget.budget else 0f,
                centerValue = "${budget.remaining}",
                centerTitle = stringResource(Res.string.diary_remaining_kcal),
                color = if (budget.remaining < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "${budget.consumed} / ${budget.budget} ${stringResource(Res.string.common_kcal)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.exerciseKcal > 0) {
                Text(
                    "+${state.exerciseKcal} ${stringResource(Res.string.common_kcal)} · " +
                        stringResource(Res.string.exercise_addback_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MacroChip(stringResource(Res.string.onb_plan_protein), state.consumed.proteinG, targets.proteinG)
                MacroChip(stringResource(Res.string.onb_plan_carbs), state.consumed.carbsG, targets.carbsG)
                MacroChip(stringResource(Res.string.onb_plan_fat), state.consumed.fatG, targets.fatG)
            }
            Spacer(Modifier.height(Spacing.md))
            PrimaryButton(
                text = stringResource(Res.string.today_add_meal),
                onClick = onAddMeal,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CartaoDaAgua(state: TodayState, aguaDaComidaMl: Int?, onAbrirDiario: () -> Unit) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(stringResource(Res.string.diary_water), style = MaterialTheme.typography.titleMedium)
                // A meta é de água total, e por isso a da comida conta para ela. Sem
                // isto pedia-se de copo o que a EFSA conta de tudo.
                val total = state.waterMl + (aguaDaComidaMl ?: 0)
                Text(
                    "$total / ${state.waterGoalMl} ml",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (total >= state.waterGoalMl) {
                        MaterialTheme.success
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    // A parcela da comida só se sabe quando metade das calorias do dia
                    // trouxeram teor de água medido — ver `AguaDaComida`. Sem ela a
                    // meta fica injusta, e o texto diz isso em vez de fingir zero.
                    aguaDaComidaMl?.let { daComida ->
                        stringResource(Res.string.today_water_parts, state.waterMl, daComida)
                    } ?: stringResource(Res.string.today_water_food_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onAbrirDiario) { Text(stringResource(Res.string.nav_diary)) }
        }
    }
}

@Composable
private fun CartaoDoTreino(treino: TodayWorkout, unidades: UnitSystem, onAbrir: () -> Unit) {
    AntaresCard(
        modifier = Modifier.fillMaxWidth().cascadeIn(0)
            .clickable(role = Role.Button, onClick = onAbrir),
    ) {
        Text(stringResource(Res.string.today_workout_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.xs))
        val texto = when {
            treino.hasActive -> stringResource(Res.string.today_workout_active)
            // O volume levava «kg» escrito no texto e o número em bruto: em imperial ficava
            // «Last workout: 9338 kg volume» a quem escolheu libras.
            treino.lastVolume != null ->
                stringResource(
                    Res.string.today_workout_last,
                    weightWithUnit(treino.lastVolume, unidades),
                )
            else -> stringResource(Res.string.today_workout_none)
        }
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = if (treino.hasActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

        treino.scheduledRoutineName?.let { nome ->
            Spacer(Modifier.height(Spacing.xs))
            Text(
                stringResource(Res.string.today_workout_scheduled, nome),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun CartaoDoJejum(sessao: FastingSessionEntity?, agoraMs: Long, onAbrir: () -> Unit) {
    AntaresCard(
        modifier = Modifier.fillMaxWidth().cascadeIn(1)
            .clickable(role = Role.Button, onClick = onAbrir),
    ) {
        Text(stringResource(Res.string.today_fasting_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.sm))
        if (sessao != null) {
            val progress = FastingMachine.progress(sessao.toSnapshot(), agoraMs)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                MiniRing(fraction = progress.fraction, reachedGoal = progress.reachedGoal)
                Column {
                    Text(
                        "${stringResource(Res.string.fasting_state_active)} · " +
                            FastingFormat.hm(progress.elapsedMs),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        if (progress.reachedGoal) {
                            stringResource(Res.string.fasting_goal_reached)
                        } else {
                            "${FastingFormat.hm(progress.remainingMs)} " +
                                stringResource(Res.string.fasting_remaining)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Text(
                stringResource(Res.string.today_fasting_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CartaoDaCorrida(corrida: RunEntity?, unidades: UnitSystem, onAbrir: () -> Unit) {
    val virgula = virgulaDecimal()
    AntaresCard(
        modifier = Modifier.fillMaxWidth().cascadeIn(2)
            .clickable(role = Role.Button, onClick = onAbrir),
    ) {
        Text(stringResource(Res.string.today_run_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.xs))
        if (corrida != null) {
            Text(
                // A distância aqui vinha sem unidade nenhuma — «Última: 3,50 · 250 kcal».
                stringResource(
                    Res.string.today_run_last,
                    "${RunFormat.distance(corrida.distanceM, unidades, virgula)} " +
                        stringResource(distanceUnitLabel(unidades)),
                    corrida.kcal,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                stringResource(Res.string.today_run_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CartaoDosPassos(passos: Long) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.health_steps, passos.toString()),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun CartaoDoPeso(state: TodayState, onRegistarPeso: () -> Unit) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.today_weight_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.sm))

        val imperial = state.unitSystem == UnitSystem.IMPERIAL
        val kgLabel = stringResource(if (imperial) Res.string.common_lb else Res.string.common_kg)

        val latest = state.latestWeightKg?.let { UnitConversions.weightToDisplay(it, state.unitSystem) }
        val trend = state.trendWeightKg?.let { UnitConversions.weightToDisplay(it, state.unitSystem) }
        if (latest != null) {
            Text("${fmtG(latest)} $kgLabel", style = MaterialTheme.typography.headlineMedium)
            trend?.let { t ->
                Text(
                    "${stringResource(Res.string.today_weight_trend)}: ${fmtG(t)} $kgLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        SecondaryButton(
            text = stringResource(Res.string.today_weight_log_cta),
            onClick = onRegistarPeso,
        )
    }
}

@Composable
private fun WeeklyBudgetCard(budget: WeeklyBudget) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.week_budget_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(
                Res.string.week_budget_consumed,
                budget.consumed,
                budget.weeklyTarget,
            ),
            style = MaterialTheme.typography.bodyLarge,
        )

        budget.perDayLeft?.let { porDia ->
            Text(
                if (budget.remaining >= 0) {
                    stringResource(Res.string.week_budget_left, porDia, budget.daysAfterToday)
                } else {
                    stringResource(Res.string.week_budget_over, -budget.remaining)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (budget.remaining >= 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
        if (!budget.complete) {
            Text(
                pluralStringResource(
                    Res.plurals.week_budget_incomplete,
                    budget.daysElapsed - budget.loggedDays,
                    budget.daysElapsed - budget.loggedDays,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DailyGapCard(gap: DailyGap, onOpen: (String) -> Unit) {
    AntaresCard(modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpen(gap.key) }) {
        Text(
            stringResource(Res.string.gap_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(
                Res.string.gap_missing,
                fmtG(gap.missing),
                Nutrients.unitOf(gap.key),
                stringResource(microLabelRes(gap.key)),
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            stringResource(Res.string.gap_cta),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * O que ficou por responder no arranque, e que a app respondeu por si para poder mostrar
 * números. Aparece por cima da meta diária de propósito: é essa meta que os palpites
 * decidem, e um palpite que ninguém sabe que lá está lê-se como uma resposta.
 *
 * Duas saídas: responder, ou dizer à app que não pergunte mais — que também é uma resposta,
 * e por isso ela não volta a insistir.
 */
@Composable
private fun RespostasEmFaltaCard(
    passos: List<OnboardingStep>,
    onAnswer: () -> Unit,
    onDismiss: () -> Unit,
) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.onb_pending_title), style = MaterialTheme.typography.titleMedium)
        val nomes = passos.map { stringResource(nomeDoPasso(it)) }
        Text(
            stringResource(Res.string.onb_pending_body, nomes.joinToString(", ")),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SecondaryButton(
                text = stringResource(Res.string.onb_pending_dismiss),
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = stringResource(Res.string.onb_pending_cta),
                onClick = onAnswer,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun nomeDoPasso(step: OnboardingStep) = when (step) {
    OnboardingStep.ACTIVITY -> Res.string.onb_pending_activity
    OnboardingStep.GOAL -> Res.string.onb_pending_goal
    OnboardingStep.GOAL_WEIGHT -> Res.string.onb_pending_goal_weight
    // Os obrigatórios nunca chegam aqui: não há como saltá-los. O ritmo é o que sobra.
    else -> Res.string.onb_pending_rate
}

@Composable
private fun StreakCard(streak: TodayStreak) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text("🔥", style = MaterialTheme.typography.headlineMedium)
            Column {
                Text(
                    pluralStringResource(Res.plurals.today_streak_days, streak.current, streak.current),
                    style = MaterialTheme.typography.titleMedium,
                )
                val subtitle = when {

                    !streak.loggedToday -> stringResource(Res.string.today_streak_keep)

                    streak.current >= streak.longest -> stringResource(Res.string.today_streak_record_now)

                    else -> pluralStringResource(Res.plurals.today_streak_record, streak.longest, streak.longest)
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (streak.freezeThisWeek) {
                    Text(
                        stringResource(Res.string.today_streak_freeze),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniRing(fraction: Float, reachedGoal: Boolean) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val color = if (reachedGoal) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(48.dp)) {
        // Decorativo: o tempo de jejum está escrito na linha ao lado do anel, e é ele que
        // o leitor de ecrã anuncia. O anel repete-o em forma.
        Canvas(modifier = Modifier.size(48.dp)) {
            val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            drawArc(color = track, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                useCenter = false,
                style = stroke,
            )
        }
    }
}

@Composable
private fun MacroChip(label: String, consumedG: Double, targetG: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${consumedG.toInt()}/${targetG}g", style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

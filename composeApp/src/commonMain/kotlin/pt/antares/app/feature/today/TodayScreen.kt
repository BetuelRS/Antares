package pt.antares.app.feature.today

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.fillMaxHeight
import pt.antares.app.core.designsystem.components.MacroBar
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
import pt.antares.app.core.database.daos.CorridaNaListaRow
import pt.antares.app.core.nutrition.DailyGap
import pt.antares.app.core.nutrition.Nutrients
import pt.antares.app.core.nutrition.microLabelRes
import pt.antares.app.core.calc.AguaDaComida
import pt.antares.app.core.designsystem.virgulaDecimal
import pt.antares.app.core.designsystem.AntaresColors
import pt.antares.app.core.designsystem.cascadeIn
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.feature.backup.AvisoDeCopiaAtrasada
import pt.antares.app.core.designsystem.distanceUnitLabel
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.MINUTES_PER_HOUR
import pt.antares.app.core.util.UnitConversions
import pt.antares.app.core.util.epochMillisToMinuteOfDay
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
    val aguaDaComida by viewModel.aguaDaComida.collectAsState()
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

    val proximoPasso = ProximoPassoCalc.escolher(
        hora = epochMillisToMinuteOfDay(nowMin) / MINUTES_PER_HOUR,
        treinoAgendado = workout.scheduledRoutineId?.let { id ->
            ProximoPasso.Treino(id, workout.scheduledRoutineName.orEmpty())
        },
        treinouHoje = workout.treinouHoje,
        treinoActivo = workout.hasActive,
        proteinaMetaG = targets.proteinG,
        proteinaConsumidaG = state.consumed.proteinG,
        aguaMetaMl = state.waterGoalMl,
        // A água da comida conta, porque a meta é de água total — é a mesma soma do cartão.
        aguaBebidaMl = state.waterMl + ((aguaDaComida as? AguaDaComida.Resultado.Medida)?.ml ?: 0),
    )

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

        // A ordem é a do esboço 01, por degraus, e é fixa (2.32.0). Até aqui a meta era o
        // quinto cartão — abaixo do aviso da cópia, do treinador, da sequência e das respostas
        // em falta —, e num telemóvel ficava abaixo da dobra: a coisa que a pessoa abriu a app
        // para ver estava fora do ecrã.
        //
        // Degrau 1: a meta do dia. Nunca abaixo de nada.
        cartao { CartaoDaMeta(targets = targets, state = state, onAddMeal = destinos.refeicao) }

        // Degrau 2: o que fazer a seguir. Só quando há — ver o `ProximoPassoCalc`.
        proximoPasso?.let { passo ->
            cartao {
                CartaoDoProximoPasso(passo, onComecar = destinos.comecarRotina, onRegistar = destinos.refeicao)
            }
        }

        // Degrau 3: o que exige decisão. O aviso da cópia à cabeça dele: quando existe é a
        // coisa mais urgente depois da meta — todo o resto se volta a registar, três anos de
        // diário não.
        cartao { AvisoDeCopiaAtrasada() }

        if (porResponder.isNotEmpty()) {
            cartao {
                RespostasEmFaltaCard(
                    passos = porResponder,
                    onAnswer = destinos.perfil,
                    onDismiss = viewModel::naoPerguntar,
                )
            }
        }

        cartao { CoachTeaserCard(onOpen = destinos.treinador) }

        dailyGap?.let { folga -> cartao { DailyGapCard(folga, onOpenGap) } }

        // Degrau 4: o que está a acontecer agora. **Os cartões vazios deixaram de existir**
        // (2.32.0): quem não está a jejuar deixou de ter um cartão a dizê-lo todos os dias. O
        // jejum começa-se agora pelo «Mais», que ganhou a porta antes de este cartão a perder.
        fasting?.let { sessao ->
            cartao { CartaoDoJejum(sessao = sessao, agoraMs = nowMin, onAbrir = destinos.jejum) }
        }
        if (workout.hasActive) {
            cartao { CartaoDoTreino(workout, state.unitSystem, destinos) }
        }

        // Degrau 5: o estado. A água e a semana ficam grandes, contra o esboço, e com razão: a
        // da água diz quando não se sabe a água da comida, e a da semana avisa quando a conta
        // está incompleta — duas das coisas que o estudo mais elogia na app, e que em meia
        // largura não cabiam. Os outros cinco são pequenos, de dois em dois (2.32.1).
        cartao {
            CartaoDaAgua(state = state, aguaDaComida = aguaDaComida, onAbrirDiario = destinos.refeicao)
        }

        weeklyBudget?.let { orcamento -> cartao { WeeklyBudgetCard(orcamento) } }

        pequeno { PequenoDoPeso(state, onAbrir = destinos.peso) }

        steps?.let { passos -> pequeno { PequenoDosPassos(passos, state) } }

        if (streak.current >= 1) {
            pequeno { PequenoDaSequencia(streak) }
        }

        // O treino sem sessão a decorrer só aparece se tiver alguma coisa para dizer — um
        // treino feito ou um agendado. A corrida, só se houver corrida. Os dois têm outra porta
        // no separador Treino, e por isso podem calar-se aqui.
        if (!workout.hasActive && (workout.lastVolume != null || workout.scheduledRoutineName != null)) {
            pequeno { PequenoDoTreino(workout, state.unitSystem, destinos) }
        }

        lastRun?.let { corrida ->
            pequeno { PequenoDaCorrida(corrida, state.unitSystem, onAbrir = destinos.corrida) }
        }
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

            // As barras do diário, e não três colunas de números: o que falta lê-se pela forma, e
            // não pela subtração «62/140g». É o mesmo componente, com as mesmas cores de
            // categoria e o excesso pela forma — dois cartões do mesmo dia não podiam desenhar os
            // mesmos macros de duas maneiras.
            Spacer(Modifier.height(Spacing.md))
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                MacroBar(
                    label = stringResource(Res.string.onb_plan_protein),
                    grams = state.consumed.proteinG,
                    targetGrams = targets.proteinG,
                    color = AntaresColors.macroProtein,
                )
                MacroBar(
                    label = stringResource(Res.string.onb_plan_carbs),
                    grams = state.consumed.carbsG,
                    targetGrams = targets.carbsG,
                    color = AntaresColors.macroCarbs,
                )
                MacroBar(
                    label = stringResource(Res.string.onb_plan_fat),
                    grams = state.consumed.fatG,
                    targetGrams = targets.fatG,
                    color = AntaresColors.macroFat,
                )
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
private fun CartaoDaAgua(
    state: TodayState,
    aguaDaComida: AguaDaComida.Resultado,
    onAbrirDiario: () -> Unit,
) {
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
                val daComida = (aguaDaComida as? AguaDaComida.Resultado.Medida)?.ml
                val total = state.waterMl + (daComida ?: 0)
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
                    //
                    // **E as duas ausências não se dizem com a mesma frase.** Quem não
                    // registou nada não tem comida por medir: tem um dia por registar, e
                    // dizer-lhe «menos de metade do que comeste» é afirmar uma coisa sobre
                    // comida que não existe.
                    when (aguaDaComida) {
                        is AguaDaComida.Resultado.Medida ->
                            stringResource(Res.string.today_water_parts, state.waterMl, aguaDaComida.ml)
                        AguaDaComida.Resultado.SemCobertura ->
                            stringResource(Res.string.today_water_food_unknown)
                        AguaDaComida.Resultado.SemRegisto ->
                            stringResource(Res.string.today_water_food_no_log)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onAbrirDiario) { Text(stringResource(Res.string.nav_diary)) }
        }
    }
}

@Composable
private fun CartaoDoTreino(treino: TodayWorkout, unidades: UnitSystem, destinos: DestinosDoHoje) {
    // Com um treino agendado por fazer, o toque começa-o: era o defeito concreto 1 da área 01 —
    // «Hoje: Peito e tríceps» levava ao painel de treino, a três toques da rotina.
    val agendado = treino.scheduledRoutineId?.takeIf { !treino.hasActive && !treino.treinouHoje }
    AntaresCard(
        modifier = Modifier.fillMaxWidth().cascadeIn(0).clickable(role = Role.Button) {
            if (agendado != null) destinos.comecarRotina(agendado) else destinos.treino()
        },
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
private fun CartaoDoJejum(sessao: FastingSessionEntity, agoraMs: Long, onAbrir: () -> Unit) {
    AntaresCard(
        modifier = Modifier.fillMaxWidth().cascadeIn(1)
            .clickable(role = Role.Button, onClick = onAbrir),
    ) {
        Text(stringResource(Res.string.today_fasting_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.sm))
        run {
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
        }
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

/**
 * A linha de «a seguir»: uma frase e o botão que a resolve. A regra está no [ProximoPassoCalc].
 */
@Composable
private fun CartaoDoProximoPasso(passo: ProximoPasso, onComecar: (String) -> Unit, onRegistar: () -> Unit) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.today_next_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    when (passo) {
                        is ProximoPasso.Treino -> stringResource(Res.string.today_next_workout, passo.nome)
                        is ProximoPasso.Proteina -> stringResource(Res.string.today_next_protein, passo.faltamG)
                        is ProximoPasso.Agua -> stringResource(Res.string.today_next_water, passo.faltamMl)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            TextButton(onClick = { if (passo is ProximoPasso.Treino) onComecar(passo.routineId) else onRegistar() }) {
                Text(
                    stringResource(
                        if (passo is ProximoPasso.Treino) Res.string.today_next_start else Res.string.today_next_log,
                    ),
                )
            }
        }
    }
}

/**
 * Um cartão de meia largura: o nome em pequeno, o número grande, e uma linha. É o molde dos
 * cinco cartões de estado do Hoje (2.32.1) — um componente só, para os cinco terem a mesma
 * altura e se lerem como uma fila, e não como cinco ideias diferentes de cartão.
 */
@Composable
private fun CartaoPequeno(
    titulo: String,
    valor: String,
    detalhe: String?,
    onClick: (() -> Unit)? = null,
    rodape: (@Composable () -> Unit)? = null,
) {
    val base = Modifier.fillMaxWidth().fillMaxHeight()
    AntaresCard(modifier = if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base) {
        Text(
            titulo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(valor, style = MaterialTheme.typography.titleLarge)
        detalhe?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        rodape?.invoke()
    }
}

@Composable
private fun PequenoDoPeso(state: TodayState, onAbrir: () -> Unit) {
    val kg = stringResource(if (state.unitSystem == UnitSystem.IMPERIAL) Res.string.common_lb else Res.string.common_kg)
    val ultimo = state.latestWeightKg?.let { UnitConversions.weightToDisplay(it, state.unitSystem) }
    val tendencia = state.trendWeightKg?.let { UnitConversions.weightToDisplay(it, state.unitSystem) }
    CartaoPequeno(
        titulo = stringResource(Res.string.today_weight_title),
        // Sem pesagem, o número grande é o convite: o cartão inteiro leva ao registo.
        valor = ultimo?.let { "${fmtG(it)} $kg" } ?: stringResource(Res.string.today_weight_log_cta),
        detalhe = tendencia?.let { "${stringResource(Res.string.today_weight_trend)}: ${fmtG(it)} $kg" },
        onClick = onAbrir,
    )
}

@Composable
private fun PequenoDosPassos(passos: Long, state: TodayState) {
    val virgula = virgulaDecimal()
    val meta = "${PassosCalc.META}"
    // A distância é uma estimativa pela passada, e escreve-se com «≈». Sem altura não aparece.
    val distancia = PassosCalc.distanciaM(passos, state.alturaCm?.toDouble())?.let { m ->
        "${RunFormat.distance(m, state.unitSystem, virgula)} ${stringResource(distanceUnitLabel(state.unitSystem))}"
    }
    CartaoPequeno(
        titulo = stringResource(Res.string.today_steps_title),
        valor = "$passos",
        detalhe = if (distancia != null) {
            stringResource(Res.string.today_steps_detail, meta, distancia)
        } else {
            stringResource(Res.string.today_steps_goal, meta)
        },
        rodape = {
            LinearProgressIndicator(
                progress = { PassosCalc.fracao(passos).coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
            )
        },
    )
}

@Composable
private fun PequenoDaSequencia(streak: TodayStreak) {
    val detalhe = when {
        !streak.loggedToday -> stringResource(Res.string.today_streak_keep)
        streak.current >= streak.longest -> stringResource(Res.string.today_streak_record_now)
        else -> pluralStringResource(Res.plurals.today_streak_record, streak.longest, streak.longest)
    }
    CartaoPequeno(
        titulo = stringResource(Res.string.today_streak_title),
        valor = "🔥 " + pluralStringResource(Res.plurals.today_streak_days, streak.current, streak.current),
        detalhe = detalhe,
        rodape = if (streak.freezeThisWeek) {
            {
                Text(
                    stringResource(Res.string.today_streak_freeze),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        } else {
            null
        },
    )
}

/** O treino em estado: o agendado por fazer primeiro, e o toque começa-o; senão, o último. */
@Composable
private fun PequenoDoTreino(treino: TodayWorkout, unidades: UnitSystem, destinos: DestinosDoHoje) {
    val agendado = treino.scheduledRoutineId?.takeIf { !treino.treinouHoje }
    val nome = treino.scheduledRoutineName
    CartaoPequeno(
        titulo = stringResource(Res.string.today_workout_title),
        valor = when {
            agendado != null && nome != null -> nome
            treino.lastVolume != null -> weightWithUnit(treino.lastVolume, unidades)
            else -> nome.orEmpty()
        },
        detalhe = stringResource(
            if (agendado != null) Res.string.today_small_workout_today else Res.string.today_small_workout_last,
        ),
        onClick = { if (agendado != null) destinos.comecarRotina(agendado) else destinos.treino() },
    )
}

@Composable
private fun PequenoDaCorrida(corrida: CorridaNaListaRow, unidades: UnitSystem, onAbrir: () -> Unit) {
    val virgula = virgulaDecimal()
    CartaoPequeno(
        titulo = stringResource(Res.string.today_run_title),
        valor = "${RunFormat.distance(corrida.distanceM, unidades, virgula)} " +
            stringResource(distanceUnitLabel(unidades)),
        detalhe = "${corrida.kcal} ${stringResource(Res.string.common_kcal)}",
        onClick = onAbrir,
    )
}

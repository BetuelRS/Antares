package pt.antares.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.components.EmptyState
import pt.antares.app.core.designsystem.components.PainelDeListaEDetalhe
import pt.antares.app.core.designsystem.components.cabeDetalheAoLado
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.painel_escolhe_da_lista
import pt.antares.app.feature.exercise.AddExerciseScreen
import pt.antares.app.feature.workout.MenuDoTreino
import pt.antares.app.feature.workout.WorkoutScreen
import pt.antares.app.feature.workout.ui.ExerciseCreateScreen
import pt.antares.app.feature.workout.ui.ExerciseDetailScreen
import pt.antares.app.feature.workout.ui.ExerciseLibraryScreen
import pt.antares.app.feature.workout.data.SessionPickBus
import pt.antares.app.feature.workout.ui.RoutineEditScreen
import pt.antares.app.feature.workout.ui.RoutineItemPickViewModel
import pt.antares.app.feature.workout.ui.WorkoutDetailScreen
import pt.antares.app.feature.workout.ui.WeeklyScheduleScreen
import pt.antares.app.feature.workout.ui.WorkoutHistoryScreen
import pt.antares.app.feature.workout.ui.WorkoutSessionScreen
import pt.antares.app.feature.workout.ui.WorkoutStatsScreen
import pt.antares.app.feature.workout.ui.WorkoutSummaryScreen

/**
 * Treinar: o treino a decorrer, as rotinas, a biblioteca de exercícios e o histórico.
 */
internal fun NavGraphBuilder.rotasDeTreino(navController: NavHostController) {
    composable<Route.Workout> {
        WorkoutScreen(
            menu = MenuDoTreino(
                biblioteca = { navController.navigate(Route.ExerciseLibrary()) },
                historico = { navController.navigate(Route.WorkoutHistory) },
                estatisticas = { navController.navigate(Route.WorkoutStats) },
                plano = { navController.navigate(Route.WorkoutSchedule) },
            ),
            onRoutine = { routineId -> navController.navigate(Route.RoutineEdit(routineId)) },
            onStartRoutine = { routineId -> navController.navigate(Route.WorkoutSession(routineId)) },
            onStartEmpty = { navController.navigate(Route.WorkoutSession()) },
            onResume = { navController.navigate(Route.WorkoutSession()) },
            onWorkout = { sessionId -> navController.navigate(Route.WorkoutDetail(sessionId)) },
            onRun = { navController.navigate(Route.Run) },
        )
    }
    composable<Route.WorkoutSchedule> {
        WeeklyScheduleScreen(onBack = { navController.popBackStack() })
    }
    composable<Route.AddExercise> { entry ->
        val route = entry.toRoute<Route.AddExercise>()
        AddExerciseScreen(
            epochDay = route.epochDay,
            onDone = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }

    rotaDaBibliotecaDeExercicios(navController)
    composable<Route.ExerciseDetail> { entry ->
        val route = entry.toRoute<Route.ExerciseDetail>()
        ExerciseDetailScreen(
            exerciseId = route.exerciseId,
            onDeleted = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }
    resto(navController)
}

/**
 * A biblioteca sai para aqui por ser a única rota com dois esquemas: sozinha numa janela
 * estreita, lista e detalhe lado a lado numa larga.
 */
private fun NavGraphBuilder.rotaDaBibliotecaDeExercicios(navController: NavHostController) {
    composable<Route.ExerciseLibrary> { entry ->
        val route = entry.toRoute<Route.ExerciseLibrary>()
        val pickVm: RoutineItemPickViewModel = koinViewModel()
        val pickBus: SessionPickBus = koinInject()
        val scope = rememberCoroutineScope()

        // Numa janela larga o detalhe abre ao lado em vez de tapar a lista: abrir um
        // exercício, voltar atrás e abrir o seguinte era o percurso mais cansativo da app
        // num tablet. A escolha guarda-se para sobreviver a uma rotação.
        //
        // Só quando a biblioteca é a biblioteca. A escolher um exercício para uma rotina ou
        // para uma sessão, o toque devolve o resultado e sai — não há detalhe para mostrar.
        val aoLado = cabeDetalheAoLado() && !route.pickMode && !route.sessionPick
        var escolhido by rememberSaveable { mutableStateOf<String?>(null) }

        val lista = @Composable {
            ExerciseLibraryScreen(
                pickMode = route.pickMode,
                onExercise = { id ->
                    when {
                        route.pickMode && route.routineId != null -> {

                            pickVm.add(route.routineId, id) { navController.popBackStack() }
                        }
                        route.sessionPick -> {

                            scope.launch { pickBus.emit(id) }
                            navController.popBackStack()
                        }
                        aoLado -> escolhido = id
                        else -> navController.navigate(Route.ExerciseDetail(id))
                    }
                },
                onCreateCustom = { navController.navigate(Route.ExerciseCreate) },
                onBack = { navController.popBackStack() },
            )
        }

        if (!aoLado) {
            lista()
        } else {
            PainelDeListaEDetalhe(
                lista = lista,
                detalhe = escolhido?.let { id ->
                    {
                        ExerciseDetailScreen(
                            exerciseId = id,
                            // Apagar um exercício personalizado só fecha o painel: a lista ao
                            // lado já se atualizou sozinha, e sair dela seria perder o sítio.
                            onDeleted = { escolhido = null },
                            onBack = { escolhido = null },
                        )
                    }
                },
                vazio = {
                    EmptyState(title = stringResource(Res.string.painel_escolhe_da_lista))
                },
            )
        }
    }
}

/** O que sobra do grafo de treino: criar exercício, rotinas, sessão, histórico e estatísticas. */
private fun NavGraphBuilder.resto(navController: NavHostController) {
    composable<Route.ExerciseCreate> {
        ExerciseCreateScreen(
            onCreated = { id ->
                navController.navigate(Route.ExerciseDetail(id)) {
                    popUpTo<Route.ExerciseCreate> { inclusive = true }
                }
            },
            onBack = { navController.popBackStack() },
        )
    }
    composable<Route.RoutineEdit> { entry ->
        val route = entry.toRoute<Route.RoutineEdit>()
        RoutineEditScreen(
            routineId = route.routineId,
            onAddExercise = { rid -> navController.navigate(Route.ExerciseLibrary(pickMode = true, routineId = rid)) },
            onStart = { rid -> navController.navigate(Route.WorkoutSession(rid)) },
            onDeleted = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
            // A cópia substitui o original na pilha: voltar atrás da cópia leva ao sítio de
            // onde se veio, e não à rotina que se copiou.
            onOpenRoutine = { rid ->
                navController.navigate(Route.RoutineEdit(rid)) {
                    popUpTo(Route.RoutineEdit(route.routineId)) { inclusive = true }
                }
            },
        )
    }

    composable<Route.WorkoutSession> { entry ->
        val route = entry.toRoute<Route.WorkoutSession>()
        WorkoutSessionScreen(
            routineId = route.routineId,
            onAddExercise = { navController.navigate(Route.ExerciseLibrary(pickMode = true, sessionPick = true)) },
            onFinished = { sessionId ->
                navController.navigate(Route.WorkoutSummary(sessionId)) {
                    popUpTo<Route.Workout>()
                }
            },
            onDiscarded = { navController.popBackStack(Route.Workout, inclusive = false) },
        )
    }
    composable<Route.WorkoutSummary> { entry ->
        val route = entry.toRoute<Route.WorkoutSummary>()
        WorkoutSummaryScreen(
            sessionId = route.sessionId,
            onDone = { navController.popBackStack(Route.Workout, inclusive = false) },
        )
    }
    composable<Route.WorkoutHistory> {
        WorkoutHistoryScreen(
            onSession = { id -> navController.navigate(Route.WorkoutDetail(id)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<Route.WorkoutDetail> { entry ->
        val route = entry.toRoute<Route.WorkoutDetail>()
        WorkoutDetailScreen(sessionId = route.sessionId, onBack = { navController.popBackStack() })
    }
    composable<Route.WorkoutStats> {
        WorkoutStatsScreen(onBack = { navController.popBackStack() })
    }
}

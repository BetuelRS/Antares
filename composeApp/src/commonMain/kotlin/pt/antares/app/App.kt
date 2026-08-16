package pt.antares.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.coach.CoachRepository
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.model.LocalMealNames
import pt.antares.app.core.model.MealNames
import pt.antares.app.feature.fooddata.FoodSeeder
import pt.antares.app.feature.profile.data.GoalMigrationRepository
import pt.antares.app.feature.fasting.data.FastingProtocolSeeder
import pt.antares.app.feature.workout.data.ExerciseSeeder
import pt.antares.app.feature.workout.data.RoutineTemplateSeeder
import pt.antares.app.core.designsystem.AntaresTheme
import pt.antares.app.core.designsystem.ThemeMode
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.LocalUndo
import pt.antares.app.core.designsystem.components.rememberUndoController
import pt.antares.app.navigation.AntaresBottomBar
import pt.antares.app.navigation.AntaresNavHost
import pt.antares.app.navigation.Route
import pt.antares.app.navigation.bottomBarRoutes

/**
 * O arranque da app. Junta o que tem de estar decidido antes de qualquer ecrã abrir — se o
 * onboarding já foi feito, o tema, os nomes das refeições — e lança em segundo plano o
 * trabalho de manutenção que não pode atrasar a primeira pintura.
 */
class AppViewModel(
    preferences: AppPreferences,
    foodSeeder: FoodSeeder,
    exerciseSeeder: ExerciseSeeder,
    templateSeeder: RoutineTemplateSeeder,
    fastingProtocolSeeder: FastingProtocolSeeder,
    coach: CoachRepository,
    goalMigration: GoalMigrationRepository,
) : ViewModel() {
    // Anulável de propósito: null é "ainda não se sabe" e mostra o ecrã de carregamento.
    // Um `false` por omissão faria a app piscar o onboarding a quem já o fez.
    //
    // `Eagerly` nos três: são lidos na primeira composição, e esperar por um subscritor
    // adiava a decisão de que ecrã abrir.
    val onboardingDone: StateFlow<Boolean?> = preferences.onboardingDone
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val mealNames: StateFlow<MealNames> = preferences.mealNames
        .map { MealNames(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MealNames())

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .map { runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    init {

        // Corrotinas separadas para o arranque não ser uma fila: o catálogo de alimentos é
        // o mais demorado, e prendia atrás dele coisas que nada têm a ver com ele.
        viewModelScope.launch { foodSeeder.seedIfNeeded() }

        // Estas duas são a exceção, e ficam juntas por ordem: as rotinas de exemplo
        // apontam a exercícios que têm de existir primeiro.
        viewModelScope.launch {
            exerciseSeeder.seedIfNeeded()
            templateSeeder.seedIfNeeded()
        }
        viewModelScope.launch { fastingProtocolSeeder.seedIfNeeded() }

        viewModelScope.launch { coach.generateIfDue() }

        viewModelScope.launch { goalMigration.onAppStart() }
    }
}

@Composable
fun App(viewModel: AppViewModel = koinViewModel()) {
    val themeMode by viewModel.themeMode.collectAsState()
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val mealNames by viewModel.mealNames.collectAsState()
    AntaresTheme(darkTheme = dark) {

        CompositionLocalProvider(LocalMealNames provides mealNames) {
            val onboardingDone by viewModel.onboardingDone.collectAsState()

            when {

                onboardingDone == null -> LoadingState()
                else -> MainScaffold(startAtOnboarding = onboardingDone == false)
            }
        }
    }
}

@Composable
private fun MainScaffold(startAtOnboarding: Boolean) {
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()
    // A barra de baixo só aparece nos ecrãs de raiz. Percorre-se a hierarquia do destino e
    // não só o destino em si: um ecrã aninhado dentro de um separador continua a pertencer-lhe.
    val rootRouteNames = bottomBarRoutes.map { it::class.qualifiedName }
    val showBottomBar = backStackEntry?.destination?.hierarchy?.any { dest ->
        dest.route in rootRouteNames
    } == true

    // O aviso com anulação vive aqui, uma vez, e chega a todos os ecrãs pelo `LocalUndo`.
    // Um por ecrã seria um em cada um dos catorze sítios que apagam, e o décimo quarto
    // ficaria por fazer.
    val snackbarHost = remember { SnackbarHostState() }
    val undo = rememberUndoController(snackbarHost)

    CompositionLocalProvider(LocalUndo provides undo) {
        AntaresScaffold(
            bottomBar = { if (showBottomBar) AntaresBottomBar(navController) },
            snackbarHost = { SnackbarHost(snackbarHost) },
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                AntaresNavHost(
                    navController = navController,
                    startDestination = if (startAtOnboarding) Route.Onboarding else Route.Today,
                )
            }
        }
    }
}

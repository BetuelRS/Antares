package pt.antares.app.navigation

import kotlinx.serialization.Serializable

/**
 * Todos os destinos da app. São tipos e não strings: os argumentos viajam com o tipo certo,
 * e um destino que deixe de existir dá erro de compilação em vez de um ecrã em branco.
 *
 * Os nomes qualificados destas classes são a identidade das rotas em execução — é por eles
 * que a barra de baixo sabe onde está —, por isso mudar um nome ou o pacote muda a rota.
 */
@Serializable
sealed interface Route {

    @Serializable
    data object Today : Route

    @Serializable
    data object Diary : Route

    @Serializable
    data object Workout : Route

    @Serializable
    data object Run : Route

    /**
     * O progresso, que era a primeira secção do separador «Eu».
     *
     * Passa a separador próprio porque estava atrás do nome menos informativo da app: o
     * `estudo/areas/14` dá-lhe 17 em 20 e chama-lhe a melhor área da Antares, e ninguém a
     * encontrava por trás de um ícone de pessoa.
     */
    @Serializable
    data object Progresso : Route

    /**
     * O que resta do «Eu» mais o menu da app, num sítio só.
     *
     * Eram dois ecrãs — o «Eu» com cinco atalhos e o menu da app com mais sete —, e o
     * `estudo/areas/19` conta as definições espalhadas por três sítios, com uma repetida.
     */
    @Serializable
    data object Mais : Route

    @Serializable
    data object ProgressPhotos : Route

    @Serializable
    data object Cycle : Route

    @Serializable
    data object Onboarding : Route

    @Serializable
    data object ProfileSettings : Route

    @Serializable
    data object HealthProfile : Route

    @Serializable
    data object BodyCompositionEdit : Route

    @Serializable
    data object ShowMaths : Route

    @Serializable
    data object MeasurementHistory : Route

    @Serializable
    data object DietBreak : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Admin : Route

    @Serializable
    data object WeightHistory : Route

    @Serializable
    data class FoodSearch(
        val slot: String,
        val epochDay: Long,
        val initial: String = "SEARCH",
        val query: String = "",
    ) : Route

    @Serializable
    data class FoodDetail(val foodId: String, val slot: String, val epochDay: Long) : Route

    @Serializable
    /**
     * A refeição e o dia viajam para o ecrã de criação para o aviso de duplicados poder
     * oferecer o alimento que já existe. Sem eles o aviso só sabia dizer que existe, e
     * mandava a pessoa voltar atrás e procurar outra vez.
     */
    data class FoodEdit(
        val foodId: String? = null,
        val barcode: String? = null,
        val slot: String? = null,
        val epochDay: Long? = null,
        // Preenchido quando se vem de uma pesquisa que não deu nada: é o que faltava ao
        // catálogo, e reescrevê-lo à mão era o passo que fazia desistir.
        val name: String? = null,
    ) : Route

    @Serializable
    data class BarcodeScan(val slot: String, val epochDay: Long) : Route

    @Serializable
    data class RecipeEdit(val recipeId: String? = null) : Route

    @Serializable
    data class RecipeDetail(val recipeId: String, val slot: String, val epochDay: Long) : Route

    @Serializable
    data object NutritionStats : Route

    @Serializable
    data class RichIn(val key: String? = null) : Route

    @Serializable
    data object Attributions : Route

    @Serializable
    data object About : Route

    /** As refeições guardadas, num sítio que é delas — proposta 5 do esboço 05. */
    @Serializable
    data object MinhasRefeicoes : Route

    @Serializable
    data object Backup : Route

    @Serializable
    data object Destinos : Route

    @Serializable
    data object CrashLog : Route

    @Serializable
    data class AddExercise(val epochDay: Long) : Route

    @Serializable
    data class ExerciseLibrary(
        val pickMode: Boolean = false,
        val routineId: String? = null,
        val sessionPick: Boolean = false,
    ) : Route

    @Serializable
    data class ExerciseDetail(val exerciseId: String) : Route

    @Serializable
    data object ExerciseCreate : Route

    @Serializable
    data class RoutineEdit(val routineId: String) : Route

    @Serializable
    data class WorkoutSession(val routineId: String? = null) : Route

    @Serializable
    data class WorkoutSummary(val sessionId: String) : Route

    @Serializable
    data object WorkoutHistory : Route

    @Serializable
    data class WorkoutDetail(val sessionId: String) : Route

    @Serializable
    data object WorkoutStats : Route

    @Serializable
    data object WorkoutSchedule : Route

    @Serializable
    data object Fasting : Route

    @Serializable
    data object FastingHistory : Route

    @Serializable
    data object RunLive : Route

    @Serializable
    data object RunSummary : Route

    @Serializable
    data object RunHistory : Route

    @Serializable
    data class RunDetail(val runId: String) : Route

    @Serializable
    data object CoachHistory : Route

    @Serializable
    data class CoachReport(val reportId: String? = null) : Route

    @Serializable
    data object HealthPermissions : Route
}

/**
 * Os cinco separadores.
 *
 * A **corrida saiu** e vive dentro do treino: são os dois atividade, e ela ocupava um quinto
 * da navegação principal para uma coisa que se faz umas vezes por mês. Entrou o **progresso**,
 * que estava escondido dentro do «Eu» — e o «Eu», que não descrevia nada, passou a «Mais».
 *
 * É a barra que o `estudo/esbocos/20-sistema-de-desenho.html` desenha.
 */
val bottomBarRoutes: List<Route> = listOf(
    Route.Today,
    Route.Diary,
    Route.Workout,
    Route.Progresso,
    Route.Mais,
)

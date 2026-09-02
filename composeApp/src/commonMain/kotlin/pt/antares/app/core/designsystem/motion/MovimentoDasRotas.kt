package pt.antares.app.core.designsystem.motion

/**
 * Que movimento leva cada ecrã.
 *
 * O mapa vive aqui e não espalhado pelos sete ficheiros de rotas, por duas razões. A
 * primeira é que o movimento descreve uma **relação** entre ecrãs, e uma relação não se lê
 * olhando para um lado só. A segunda é que assim há um sítio onde se vê a app inteira de
 * uma vez, e se percebe que o Diário e o Hoje são irmãos enquanto o Detalhe de um alimento
 * está mais fundo — que é a única maneira de isto não virar uma colecção de escolhas soltas.
 *
 * As chaves são os nomes simples das rotas. O `MovimentoDeTodasAsRotasTest` percorre o
 * `Routes.kt` e exige que **todas** estejam aqui: uma rota nova sem movimento não fica com
 * um valor por omissão em silêncio — fica com o teste vermelho e com a decisão por tomar.
 */
object MovimentoDasRotas {

    private val RESTANTES = listOf(
        // A corrida está aqui desde a 2.20.1: deixou de ser separador e passou a ser um
        // degrau a partir do painel de treino, como a biblioteca ou o histórico.
        "Run", "ProgressPhotos", "Cycle", "ProfileSettings", "HealthProfile",
        "BodyCompositionEdit", "ShowMaths", "MeasurementHistory", "DietBreak", "Settings",
        "Admin", "WeightHistory", "FoodSearch", "FoodDetail", "FoodEdit", "RecipeEdit",
        "RecipeDetail", "MinhasRefeicoes", "NutritionStats", "RichIn", "Attributions", "About", "Backup",
        "Destinos", "CrashLog", "AddExercise", "ExerciseLibrary", "ExerciseDetail",
        "ExerciseCreate", "RoutineEdit", "WorkoutHistory", "WorkoutDetail", "WorkoutStats",
        "WorkoutSchedule", "FastingHistory", "RunHistory", "RunDetail", "CoachHistory",
        "HealthPermissions",
    )

    private val porNome: Map<String, Movimento> = buildMap {
        // Os cinco separadores. Ninguém está mais fundo do que ninguém.
        for (r in listOf("Today", "Diary", "Workout", "Progresso", "Mais")) put(r, Movimento.ENTRE_IRMAOS)

        // Modos em que se entra e de que se sai por baixo.
        put("BarcodeScan", Movimento.DE_BAIXO)

        // Sessões: não se está a navegar, está-se a entrar noutro estado da app.
        for (r in listOf("WorkoutSession", "RunLive", "Fasting", "Onboarding")) {
            put(r, Movimento.MERGULHO)
        }

        // Resultados. Acontecem uma vez por sessão e podem dar-se ao luxo de assentar.
        for (r in listOf("WorkoutSummary", "RunSummary", "CoachReport")) put(r, Movimento.RESULTADO)

        // Tudo o resto é um degrau para dentro. É a maioria, e é bom que seja: uma app onde
        // metade dos ecrãs tem um movimento especial não tem vocabulário nenhum.
        for (r in RESTANTES) put(r, Movimento.MAIS_FUNDO)
    }


    /**
     * O movimento de uma rota, pelo nome com que o navegador a conhece — que é o nome
     * qualificado da classe, com pacote e tudo, e às vezes com os argumentos atrás.
     *
     * Quando não reconhece, devolve [Movimento.MAIS_FUNDO]. Não é para tapar buracos: é para
     * a app nunca ficar sem animação por causa de um nome que mudou de forma. O buraco é
     * apanhado pelo teste, que lê o `Routes.kt` e não os nomes em tempo de execução.
     */
    fun de(rotaQualificada: String?): Movimento {
        val simples = rotaQualificada
            ?.substringBefore('/')
            ?.substringBefore('?')
            ?.substringAfterLast('.')
            ?: return Movimento.MAIS_FUNDO
        return porNome[simples] ?: Movimento.MAIS_FUNDO
    }

    /** Os nomes classificados, para o teste os poder comparar com o `Routes.kt`. */
    val nomes: Set<String> get() = porNome.keys
}

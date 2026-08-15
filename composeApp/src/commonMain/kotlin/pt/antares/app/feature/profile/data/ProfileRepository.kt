package pt.antares.app.feature.profile.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import pt.antares.app.core.calc.GoalHistoryCalc
import pt.antares.app.core.calc.NutritionCalc
import pt.antares.app.core.calc.ProteinFloor
import pt.antares.app.core.calc.Targets
import pt.antares.app.core.coach.CoachRepository
import pt.antares.app.core.database.daos.DailyTargetOverrideDao
import pt.antares.app.core.database.daos.FoodLogDao
import pt.antares.app.core.database.daos.GoalHistoryDao
import pt.antares.app.core.database.daos.UserProfileDao
import pt.antares.app.core.database.daos.WeightLogDao
import pt.antares.app.core.database.daos.WorkoutSessionDao
import pt.antares.app.core.database.entities.GoalHistoryEntity
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import kotlinx.datetime.Clock
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.todayEpochDay
import kotlin.math.roundToInt

/**
 * O perfil, o peso e as metas. Junta os três porque nenhum deles se lê sozinho: uma meta
 * depende do perfil e da última pesagem, e mudar qualquer um recalcula-a.
 */
class ProfileRepository(
    private val profileDao: UserProfileDao,
    private val weightDao: WeightLogDao,
    private val overrideDao: DailyTargetOverrideDao,
    private val foodLogDao: FoodLogDao,
    private val goalDao: GoalHistoryDao,
    // Só para saber se há hábito de treino de força, que sobe o chão de proteína em
    // défice — ver [ProteinFloor]. O nível de atividade não serve: um trabalhador da
    // construção é muito ativo e não treina.
    private val workoutSessionDao: WorkoutSessionDao,
    private val io: CoroutineDispatcher,
) {

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    private fun inicioDaJanelaDeTreino(): Long =
        now() - ProteinFloor.TRAINED_WINDOW_WEEKS * DAYS_PER_WEEK * MS_PER_DAY

    fun observeProfile(): Flow<UserProfileEntity?> = profileDao.observe()

    suspend fun saveProfile(profile: UserProfileEntity) = withContext(io) {
        profileDao.upsert(profile.copy(updatedAt = now()))

        recordGoalIfNew(profile.goalWeightKg)
    }

    private suspend fun recordGoalIfNew(newTargetKg: Double?) {
        val anterior = goalDao.latest()?.targetKg
        if (!GoalHistoryCalc.shouldRecord(anterior, newTargetKg)) return
        goalDao.upsert(
            GoalHistoryEntity(
                id = Ids.newUuid(),
                targetKg = newTargetKg!!,
                setOnEpochDay = todayEpochDay(),

                startWeightKg = weightDao.latest()?.weightKg,
                updatedAt = now(),
            ),
        )
    }

    fun observeWeights(): Flow<List<WeightLogEntity>> = weightDao.observeAll()

    fun observeLatestWeight(): Flow<WeightLogEntity?> = weightDao.observeLatest()

    suspend fun upsertWeight(
        epochDay: Long,
        weightKg: Double,
        note: String?,

        averageWithExisting: Boolean = true,
    ) = withContext(io) {

        val existing = weightDao.byDayForWrite(epochDay)

        // Duas pesagens no mesmo dia dão a média, e não a última: quem se pesa de manhã e
        // à noite tem duas medições igualmente válidas do mesmo dia. O parâmetro deixa a
        // importação e a correção manual substituírem em vez de misturar.
        val vivo = existing?.takeIf { !it.deleted }
        val value = if (averageWithExisting && vivo != null) {
            (vivo.weightKg + weightKg) / 2.0
        } else {
            weightKg
        }
        weightDao.upsert(
            WeightLogEntity(
                id = existing?.id ?: Ids.newUuid(),
                epochDay = epochDay,
                weightKg = value,
                note = note,
                updatedAt = now(),
            ),
        )
    }

    /**
     * Devolve o peso anterior quando o novo parece um engano de digitação — um zero a mais,
     * ou libras onde deviam ser quilos. Só avisa: quem confirma é a pessoa.
     */
    suspend fun weightLooksLikeTypo(weightKg: Double): Double? = withContext(io) {
        // Sem histórico não há como suspeitar de nada, e a primeira pesagem passa sempre.
        val reference = weightDao.latest()?.weightKg ?: return@withContext null
        val ratio = weightKg / reference
        if (ratio < TYPO_LOW_RATIO || ratio > TYPO_HIGH_RATIO) reference else null
    }

    suspend fun deleteWeight(id: String) = withContext(io) {
        weightDao.softDelete(id, now())
    }

    fun observeTargets(epochDay: Long = todayEpochDay()): Flow<Targets?> =
        combine(
            profileDao.observe(),
            weightDao.observeLatest(),
            overrideDao.observeByDay(epochDay),
            workoutSessionDao.observeFinishedSince(inicioDaJanelaDeTreino()),
        ) { profile, weight, override, treinos ->
            when {
                // Sem perfil não há metas nenhumas: é o que mantém a app no onboarding.
                profile == null -> null
                // Uma meta fixada para o dia manda sobre o cálculo — vem de uma proposta
                // adaptativa aceite. Note-se que sai sem [Targets.energy]: o desdobramento
                // da conta não se aplica a um número que não foi calculado agora.
                override != null -> Targets(
                    kcal = override.kcal,
                    proteinG = override.proteinG,
                    carbsG = override.carbsG,
                    fatG = override.fatG,
                )
                else -> NutritionCalc.dailyTargets(
                    profile = profile,
                    weightKg = weight?.weightKg ?: DEFAULT_WEIGHT_KG,
                    todayEpochDay = epochDay,
                    treinaForca = ProteinFloor.treinaForca(treinos),
                )
            }
        }

    suspend fun targetsFor(epochDay: Long): Targets? = withContext(io) {
        val profile = profileDao.get() ?: return@withContext null
        overrideDao.byDay(epochDay)?.let {
            return@withContext Targets(kcal = it.kcal, proteinG = it.proteinG, carbsG = it.carbsG, fatG = it.fatG)
        }
        val weight = weightDao.latest()?.weightKg ?: DEFAULT_WEIGHT_KG
        NutritionCalc.dailyTargets(
            profile = profile,
            weightKg = weight,
            todayEpochDay = epochDay,
            treinaForca = ProteinFloor.treinaForca(
                workoutSessionDao.finishedSince(inicioDaJanelaDeTreino()),
            ),
        )
    }

    /**
     * O gasto que a app aprendeu com o histórico, ou null se a meta deste dia não veio de
     * uma proposta adaptativa. Deduz-se da meta menos o ritmo, porque é assim que ele foi
     * gravado — ver [AdaptiveTdee.Proposal].
     */
    suspend fun learnedTdee(epochDay: Long = todayEpochDay()): Int? = withContext(io) {
        val profile = profileDao.get() ?: return@withContext null
        val override = overrideDao.byDay(epochDay) ?: return@withContext null
        // A origem tem de ser verificada: uma meta fixada à mão não ensinou nada à app.
        if (override.source != CoachRepository.SOURCE_ADAPTIVE) return@withContext null
        override.kcal - profile.goalRateKcal
    }

    suspend fun loggedDaysPerWeek(weeks: Int, today: Long = todayEpochDay()): Int = withContext(io) {
        if (weeks <= 0) return@withContext 0
        val from = today - weeks * 7L + 1
        val dias = foodLogDao.loggedDaysSince(from).count { it in from..today }
        (dias.toDouble() / weeks).roundToInt()
    }

    suspend fun profileOnce(): UserProfileEntity? = withContext(io) { profileDao.get() }

    suspend fun weightsChronological(): List<Pair<Long, Double>> = withContext(io) {
        weightDao.exportRows().sortedBy { it.epochDay }.map { it.epochDay to it.weightKg }
    }

    /** Quantas semanas distintas já tiveram uma proposta aceite. Vai para o ecrã "Sobre". */
    suspend fun adaptiveWeeks(): Int = withContext(io) {
        overrideDao.exportRows()
            .filter { it.source == CoachRepository.SOURCE_ADAPTIVE }
            // Divisão inteira por sete agrupa por semana. Não coincide com a segunda-feira
            // do calendário, mas conta cada semana uma vez, que é o que interessa.
            .map { it.epochDay / 7 }
            .distinct()
            .size
    }

    companion object {

        const val DEFAULT_WEIGHT_KG = 70.0

        // Um quarto do peso para cima ou para baixo. Nenhuma pessoa muda tanto entre duas
        // pesagens, e o intervalo é largo que chegue para não incomodar quem perdeu muito.
        const val TYPO_LOW_RATIO = 0.75
        const val TYPO_HIGH_RATIO = 1.25

        private const val DAYS_PER_WEEK = 7L
        private const val MS_PER_DAY = 86_400_000L
    }
}

package pt.antares.app.feature.workout.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.workout_seed_full_body_a
import pt.antares.app.generated.resources.workout_seed_full_body_b
import pt.antares.app.generated.resources.workout_seed_legs
import pt.antares.app.generated.resources.workout_seed_lower
import pt.antares.app.generated.resources.workout_seed_pull
import pt.antares.app.generated.resources.workout_seed_push
import pt.antares.app.generated.resources.workout_seed_upper
import pt.antares.app.core.database.daos.ExerciseLibraryDao
import pt.antares.app.core.database.daos.RoutineDao
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.RoutineItemEntity
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.TextNormalize

class RoutineTemplateSeeder(
    private val routineDao: RoutineDao,
    private val exerciseDao: ExerciseLibraryDao,
    private val io: CoroutineDispatcher,
) {

    private data class Ex(val id: String, val fallbackTerm: String)
    private data class Template(val nome: StringResource, val exercises: List<Ex>)

    private companion object {
        val BENCH = Ex("Barbell_Bench_Press_-_Medium_Grip", "bench press")
        val INCLINE = Ex("Barbell_Incline_Bench_Press_-_Medium_Grip", "incline bench press")
        val SQUAT = Ex("Barbell_Squat", "squat")
        val DEADLIFT = Ex("Barbell_Deadlift", "deadlift")
        val RDL = Ex("Romanian_Deadlift", "romanian deadlift")
        val ROW = Ex("Bent_Over_Barbell_Row", "bent over barbell row")
        val OHP = Ex("Barbell_Shoulder_Press", "shoulder press")
        val CURL = Ex("Barbell_Curl", "biceps curl")
        val PUSHDOWN = Ex("Triceps_Pushdown", "triceps pushdown")
        val PULLDOWN = Ex("Wide-Grip_Lat_Pulldown", "lat pulldown")
        val LEGPRESS = Ex("Leg_Press", "leg press")
        val LATRAISE = Ex("Side_Lateral_Raise", "lateral raise")
        val PULLUP = Ex("Pullups", "pull-up")
        val LUNGE = Ex("Barbell_Walking_Lunge", "lunge")
        val CALF = Ex("Standing_Calf_Raises", "calf raise")
    }

    /**
     * Os nomes vêm dos recursos e não do código.
     *
     * Eram sete literais — seis em inglês e «Pernas» em português —, e viam-se assim nas duas
     * línguas da app: quem abria a app em inglês encontrava «Pernas» a meio da lista. Ficou
     * medido na 2.20.0 e registado como troca por decidir.
     *
     * **O nome fica congelado na língua do primeiro arranque**, e é de propósito: a partir do
     * momento em que a rotina existe ela é da pessoa, com nome que ela pode mudar — e reescrever
     * nomes de rotinas ao trocar de idioma mexia numa coisa que já não é da app.
     */
    private val templates = listOf(
        Template(Res.string.workout_seed_full_body_a, listOf(BENCH, SQUAT, ROW, OHP, CURL)),
        Template(Res.string.workout_seed_full_body_b, listOf(DEADLIFT, INCLINE, PULLDOWN, LEGPRESS, PUSHDOWN)),
        Template(Res.string.workout_seed_push, listOf(BENCH, OHP, INCLINE, LATRAISE, PUSHDOWN)),
        Template(Res.string.workout_seed_pull, listOf(DEADLIFT, PULLDOWN, ROW, PULLUP, CURL)),
        Template(Res.string.workout_seed_legs, listOf(SQUAT, LEGPRESS, RDL, LUNGE, CALF)),
        Template(Res.string.workout_seed_upper, listOf(BENCH, ROW, OHP, PULLDOWN, CURL, PUSHDOWN)),
        Template(Res.string.workout_seed_lower, listOf(SQUAT, RDL, LEGPRESS, LUNGE, CALF)),
    )

    suspend fun seedIfNeeded() = withContext(io) {
        if (routineDao.countRoutines() > 0) return@withContext
        val now = Clock.System.now().toEpochMilliseconds()

        templates.forEachIndexed { rIndex, template ->
            val routineId = Ids.newUuid()
            val items = mutableListOf<RoutineItemEntity>()
            template.exercises.forEach { ex ->

                val match = exerciseDao.byId(ex.id)
                    ?: exerciseDao.findFirstBySearch(TextNormalize.normalize(ex.fallbackTerm))
                    ?: return@forEach
                items += RoutineItemEntity(
                    id = Ids.newUuid(),
                    routineId = routineId,
                    exerciseId = match.id,
                    targetSets = 3,
                    targetRepsMin = 8,
                    targetRepsMax = 12,
                    targetWeightKg = null,
                    restSec = 90,
                    position = items.size,
                    supersetGroup = null,
                    updatedAt = now,
                )
            }
            if (items.isEmpty()) return@forEachIndexed
            routineDao.upsertRoutine(
                RoutineEntity(
                    id = routineId,
                    name = getString(template.nome),
                    note = null,
                    position = rIndex,
                    updatedAt = now,
                ),
            )
            routineDao.upsertItems(items)
        }
    }
}

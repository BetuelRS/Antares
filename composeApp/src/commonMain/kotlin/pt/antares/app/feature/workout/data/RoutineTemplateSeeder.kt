package pt.antares.app.feature.workout.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
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
    private data class Template(val name: String, val exercises: List<Ex>)

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

    private val templates = listOf(
        Template("Full Body A", listOf(BENCH, SQUAT, ROW, OHP, CURL)),
        Template("Full Body B", listOf(DEADLIFT, INCLINE, PULLDOWN, LEGPRESS, PUSHDOWN)),
        Template("Push", listOf(BENCH, OHP, INCLINE, LATRAISE, PUSHDOWN)),
        Template("Pull", listOf(DEADLIFT, PULLDOWN, ROW, PULLUP, CURL)),
        Template("Pernas", listOf(SQUAT, LEGPRESS, RDL, LUNGE, CALF)),
        Template("Upper", listOf(BENCH, ROW, OHP, PULLDOWN, CURL, PUSHDOWN)),
        Template("Lower", listOf(SQUAT, RDL, LEGPRESS, LUNGE, CALF)),
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
                    dirty = false,
                )
            }
            if (items.isEmpty()) return@forEachIndexed
            routineDao.upsertRoutine(
                RoutineEntity(
                    id = routineId,
                    name = template.name,
                    note = null,
                    position = rIndex,
                    updatedAt = now,
                    dirty = false,
                ),
            )
            routineDao.upsertItems(items)
        }
    }
}

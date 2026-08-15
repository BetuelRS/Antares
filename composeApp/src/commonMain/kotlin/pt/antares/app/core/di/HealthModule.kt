package pt.antares.app.core.di

import org.koin.dsl.module
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pt.antares.app.core.database.daos.ExerciseLogDao
import pt.antares.app.core.database.daos.RunDao
import pt.antares.app.core.database.daos.WorkoutSessionDao
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.health.DayNutrition
import pt.antares.app.core.health.HealthPublisher
import pt.antares.app.core.health.HealthRepository
import pt.antares.app.core.health.NoHealthGateway
import pt.antares.app.core.health.OutboundBodyComposition
import pt.antares.app.core.health.OutboundKind
import pt.antares.app.core.health.OutboundSession
import pt.antares.app.core.health.TimeWindow
import pt.antares.app.core.notifications.NoopCoachNotifier
import pt.antares.app.core.util.toEpochDay
import pt.antares.app.core.coach.CoachRepository
import pt.antares.app.core.database.daos.FoodLogDao
import pt.antares.app.core.database.daos.WeightLogDao
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.feature.stats.NutritionStatsRepository
import pt.antares.app.feature.profile.data.BodyMeasurementRepository
import pt.antares.app.core.model.BodyFatSource.MEASURED
import pt.antares.app.core.database.daos.BodyMeasurementDao

/**
 * O que lê e escreve fora da app com autorização: Health Connect, nos dois sentidos, e o
 * treinador que resume a semana.
 *
 * As três montagens são longas porque passam funções em vez de DAOs: o que cada uma pode
 * ler e escrever fica à vista aqui, e não escondido dentro da classe.
 */
val healthModule = module {
    single {
        CoachRepository(
            coachDao = get(),
            foodLogDao = get(),
            weightDao = get(),
            profileDao = get(),
            overrideDao = get(),
            workoutSessionDao = get(),
            workoutSetDao = get(),
            exerciseLogDao = get(),
            fastingDao = get(),
            runDao = get(),
            statsRepository = get(),
            prefs = get(),
            notifier = getOrNull() ?: NoopCoachNotifier(),
            io = get(IoDispatcher),
        )
    }

    single {
        val weightDao = get<WeightLogDao>()
        val exerciseLogDao = get<ExerciseLogDao>()
        val workoutDao = get<WorkoutSessionDao>()
        val runDao = get<RunDao>()

        HealthRepository(
            // `getOrNull` porque o gateway só existe no módulo Android e só quando o
            // serviço está instalado. Sem ele, o [NoHealthGateway] devolve vazio em vez de
            // a app rebentar ao arrancar.
            gateway = getOrNull() ?: NoHealthGateway,
            weights = object : HealthRepository.WeightWriter {
                override suspend fun importedRefs() = weightDao.importedRefs().toSet()
                override suspend fun existsOnDay(epochDay: Long) = weightDao.byDay(epochDay) != null
                override suspend fun insert(entry: WeightLogEntity) = weightDao.upsert(entry)
            },
            exercise = object : HealthRepository.ExerciseWriter {
                override suspend fun importedRefs() = exerciseLogDao.importedRefs().toSet()
                override suspend fun insert(log: ExerciseLogEntity) = exerciseLogDao.upsert(log)
            },

            // Treinos e corridas juntos: para efeitos de duplicação são a mesma coisa, e
            // uma corrida da app também é publicada por um relógio que a acompanhe.
            ownWindows = { fromMs ->
                val workouts = workoutDao.endedSince(fromMs)
                    .mapNotNull { s -> s.endedAt?.let { TimeWindow(s.startedAt, it) } }
                val runs = runDao.runsBetween(fromMs, Long.MAX_VALUE)
                    .map { TimeWindow(it.startedAt, it.endedAt) }
                workouts + runs
            },
            latestWeightKg = { weightDao.latest()?.weightKg },
            lastImportAt = { get<AppPreferences>().lastHealthImportAtOnce() },
            setLastImportAt = { get<AppPreferences>().setLastHealthImportAt(it) },
            io = get(IoDispatcher),
            now = { Clock.System.now().toEpochMilliseconds() },
            epochDayOf = { ms ->
                Instant.fromEpochMilliseconds(ms)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .toEpochDay()
            },

            measurements = { day, pct ->
                get<BodyMeasurementRepository>()
                    .record(
                        epochDay = day,
                        bodyFatPct = pct,
                        // Vem de uma balança de bioimpedância, e por isso conta como medida:
                        // é o que a torna utilizável para o basal por massa magra.
                        bodyFatSource = MEASURED,
                    )
            },
        )
    }

    single {
        val foodDao = get<FoodLogDao>()
        val exerciseLogDao = get<ExerciseLogDao>()
        val workoutDao = get<WorkoutSessionDao>()
        val runDao = get<RunDao>()
        val measurementDao = get<BodyMeasurementDao>()
        val weightDao = get<WeightLogDao>()

        HealthPublisher(
            gateway = getOrNull() ?: NoHealthGateway,
            nutrition = { fromDay ->
                val stats = get<NutritionStatsRepository>()
                foodDao.loggedDaysSince(fromDay).map { day ->
                    val t = foodDao.dayTotals(day)

                    DayNutrition(day, t.kcal, t.proteinG, t.carbsG, t.fatG, stats.totals(day, day).byKey)
                }
            },
            sessions = { fromMs ->
                val runs = runDao.runsBetween(fromMs, Long.MAX_VALUE).map {
                    OutboundSession(
                        clientId = it.id,
                        kind = OutboundKind.RUN,
                        title = it.name,
                        startMs = it.startedAt,
                        endMs = it.endedAt,
                        kcal = it.kcal,
                    )
                }
                val workouts = workoutDao.endedSince(fromMs).mapNotNull { s ->
                    val end = s.endedAt ?: return@mapNotNull null

                    // As calorias do treino não estão na sessão: vivem na linha de exercício
                    // que ela gerou, e é por ali que se vão buscar.
                    val kcal = exerciseLogDao.byRef(s.id)?.kcal ?: 0
                    OutboundSession(
                        clientId = s.id,
                        kind = OutboundKind.WORKOUT,
                        title = s.note?.takeIf { it.isNotBlank() } ?: "Treino",
                        startMs = s.startedAt,
                        endMs = end,
                        kcal = kcal,
                    )
                }
                runs + workouts
            },

            bodyComposition = { fromDay ->
                val pesos = weightDao.exportRows().sortedBy { it.epochDay }
                measurementDao.all()
                    .filter { it.epochDay >= fromDay }
                    .map { m ->

                        val peso = pesos.lastOrNull { it.epochDay <= m.epochDay }?.weightKg
                        OutboundBodyComposition(
                            epochDay = m.epochDay,
                            bodyFatPct = m.bodyFatPct,
                            leanMassKg = if (peso != null && m.bodyFatPct != null) {
                                peso * (1.0 - m.bodyFatPct!! / 100.0)
                            } else {
                                null
                            },
                        )
                    }
            },
            lastPublishAt = { get<AppPreferences>().lastHealthPublishAtOnce() },
            setLastPublishAt = { get<AppPreferences>().setLastHealthPublishAt(it) },
            io = get(IoDispatcher),
            now = { Clock.System.now().toEpochMilliseconds() },
        )
    }
}

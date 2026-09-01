package pt.antares.app.core.di

import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.privacy.DataExporter
import pt.antares.app.core.privacy.PrivacyRepository
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class KoinGraphTest : KoinTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `o grafo de DI resolve motor de sync, exportacao e privacidade`() {
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(coreModule, databaseModule, viewModelModule)
        }

        assertNotNull(get<pt.antares.app.core.network.supabase.AnonymousSession>())
        assertNotNull(get<DataExporter>())
        assertNotNull(get<PrivacyRepository>())
    }

    @Test
    fun `todos os repositorios do grafo constroem`() {
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(coreModule, databaseModule, viewModelModule)
        }

        val falhas = mutableListOf<String>()
        for ((nome, obter) in repositorios()) {
            runCatching(obter).onFailure { falhas += "$nome: ${it.message?.lineSequence()?.first()}" }
        }
        assertEquals(emptyList(), falhas, "não constroem — rebentariam ao abrir o ecrã:")
    }

    private fun repositorios(): List<Pair<String, () -> Any>> = listOf(
        "CycleRepository" to { get<pt.antares.app.feature.profile.data.CycleRepository>() },
        "ProfileRepository" to { get<pt.antares.app.feature.profile.data.ProfileRepository>() },
        "BodyMeasurementRepository" to { get<pt.antares.app.feature.profile.data.BodyMeasurementRepository>() },
        "ProgressRepository" to { get<pt.antares.app.feature.progress.ProgressRepository>() },
        "ProgressPhotoRepository" to { get<pt.antares.app.feature.progress.ProgressPhotoRepository>() },
        "DiaryRepository" to { get<pt.antares.app.feature.diary.DiaryRepository>() },
        "FoodRepository" to { get<pt.antares.app.feature.fooddata.FoodRepository>() },
        "CoachRepository" to { get<pt.antares.app.core.coach.CoachRepository>() },
        "ExerciseRepository" to { get<pt.antares.app.feature.exercise.ExerciseRepository>() },
        "NutritionStatsRepository" to { get<pt.antares.app.feature.stats.NutritionStatsRepository>() },
        "WorkoutHubRepository" to { get<pt.antares.app.feature.workout.data.WorkoutHubRepository>() },
    )
}

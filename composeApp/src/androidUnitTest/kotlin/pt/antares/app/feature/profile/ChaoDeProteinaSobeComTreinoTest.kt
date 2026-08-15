package pt.antares.app.feature.profile

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.calc.ProteinFloor
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.Sex
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.testing.Fabricas
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * O chão de proteína era 1,8 g por quilo de massa magra para toda a gente em défice. Helms et
 * al. (2014, IJSNEM) pede 2,3 a 3,1 a quem treina força em restrição.
 *
 * O que se prova aqui é que a app repara no treino — e que repara **no treino**, e não no nível
 * de atividade declarado. Os dois perfis destes testes são idênticos até à última vírgula; a
 * única diferença é o histórico de treinos na base.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChaoDeProteinaSobeComTreinoTest : ViewModelHarness() {

    private val agora = Clock.System.now().toEpochMilliseconds()
    private val umDia = 86_400_000L

    /** Massa magra medida: sem ela o chão não escala, porque Helms é por quilo de magra. */
    private suspend fun pessoaEmDefice(ritmoKcal: Int) {
        db.weightLogDao().upsert(
            WeightLogEntity(id = "w", epochDay = 20_000L, weightKg = 80.0, note = null, updatedAt = agora),
        )
        db.userProfileDao().upsert(
            UserProfileEntity(
                sex = Sex.MALE,
                birthEpochDay = 10_000L,
                heightCm = 180,
                activityLevel = ActivityLevel.MODERATE,
                goalType = GoalType.LOSE,
                goalRateKcal = ritmoKcal,
                macroStrategy = MacroStrategy.BALANCED,
                customProteinG = null,
                customCarbsG = null,
                customFatG = null,
                bodyFatPct = 20.0,
                bodyFatSource = BodyFatSource.MEASURED,
                updatedAt = agora,
            ),
        )
    }

    private suspend fun treinosAcabados(quantos: Int) {
        for (i in 0 until quantos) {
            db.workoutSessionDao().upsertSession(
                WorkoutSessionEntity(
                    id = "s-$i",
                    startedAt = agora - (i + 1) * umDia,
                    endedAt = agora - (i + 1) * umDia + 3_600_000L,
                    routineId = null,
                    note = null,
                    status = SessionStatus.DONE,
                    updatedAt = agora,
                ),
            )
        }
    }

    private suspend fun proteinaAlvo(): Int {
        val alvos = Fabricas.profileRepository(db, dispatcher).targetsFor(20_000L)
        return assertNotNull(alvos).proteinG
    }

    @Test
    fun `sem historico de treino a meta fica onde sempre esteve`() = runTest(dispatcher) {
        pessoaEmDefice(ritmoKcal = -500)

        val semTreino = proteinaAlvo()

        treinosAcabados(ProteinFloor.TRAINED_MIN_SESSIONS)
        val comTreino = proteinaAlvo()

        assertTrue(
            comTreino > semTreino,
            "seis treinos em quatro semanas é hábito, e Helms fala de pessoas treinadas. " +
                "Sem treino $semTreino g, com treino $comTreino g",
        )
    }

    @Test
    fun `um treino solto nao conta como habito`() = runTest(dispatcher) {
        pessoaEmDefice(ritmoKcal = -500)
        val semNada = proteinaAlvo()

        treinosAcabados(ProteinFloor.TRAINED_MIN_SESSIONS - 1)

        assertEquals(
            semNada,
            proteinaAlvo(),
            "ter ido ao ginásio não é treinar: abaixo do limiar a meta não pode mexer",
        )
    }

    @Test
    fun `defice mais fundo pede mais proteina a quem treina`() = runTest(dispatcher) {
        treinosAcabados(ProteinFloor.TRAINED_MIN_SESSIONS)

        pessoaEmDefice(ritmoKcal = -200)
        val leve = proteinaAlvo()

        pessoaEmDefice(ritmoKcal = -700)
        val fundo = proteinaAlvo()

        assertTrue(
            fundo > leve,
            "quanto menos calorias entram, mais o corpo vai buscar músculo. " +
                "Leve $leve g, fundo $fundo g",
        )
    }

    @Test
    fun `o treino nao muda a meta de quem nao esta em defice`() = runTest(dispatcher) {
        db.weightLogDao().upsert(
            WeightLogEntity(id = "w", epochDay = 20_000L, weightKg = 80.0, note = null, updatedAt = agora),
        )
        db.userProfileDao().upsert(
            UserProfileEntity(
                sex = Sex.MALE,
                birthEpochDay = 10_000L,
                heightCm = 180,
                activityLevel = ActivityLevel.MODERATE,
                goalType = GoalType.MAINTAIN,
                goalRateKcal = 0,
                macroStrategy = MacroStrategy.BALANCED,
                customProteinG = null,
                customCarbsG = null,
                customFatG = null,
                bodyFatPct = 20.0,
                bodyFatSource = BodyFatSource.MEASURED,
                updatedAt = agora,
            ),
        )
        val semTreino = proteinaAlvo()

        treinosAcabados(ProteinFloor.TRAINED_MIN_SESSIONS)

        assertEquals(
            semTreino,
            proteinaAlvo(),
            "o intervalo de Helms é sobre restrição calórica; fora dela não se aplica",
        )
    }
}

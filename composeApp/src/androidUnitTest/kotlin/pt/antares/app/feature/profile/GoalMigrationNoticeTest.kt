package pt.antares.app.feature.profile

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.calc.GoalChangeReason
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.Sex
import pt.antares.app.feature.profile.data.GoalMigrationRepository
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O aviso de que a meta mudou de cálculo aparece uma vez só, e só a quem tem passado para
 * comparar. Quem instala a app hoje não pode receber a explicação de uma mudança que nunca viu —
 * e quem já a usava não pode deixar de a receber, senão o número novo parece um defeito.
 *
 * A deteção da mudança em si é do `ProfileMigrationTest`; o que aqui se afirma é a porta que
 * decide a quem se fala.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GoalMigrationNoticeTest : ViewModelHarness() {

    private fun repository() = GoalMigrationRepository(
        db.userProfileDao(),
        db.weightLogDao(),
        prefs,
        dispatcher,
    )

    private suspend fun perfil(
        exerciseAddBack: Boolean = true,
        activityLevel: ActivityLevel = ActivityLevel.SEDENTARY,
    ) {
        db.userProfileDao().upsert(
            UserProfileEntity(
                sex = Sex.MALE,
                birthEpochDay = 10_000L,
                heightCm = 178,
                activityLevel = activityLevel,
                goalType = GoalType.MAINTAIN,
                goalRateKcal = 0,
                macroStrategy = MacroStrategy.BALANCED,
                customProteinG = null,
                customCarbsG = null,
                customFatG = null,
                exerciseAddBack = exerciseAddBack,
                updatedAt = 0L,
            ),
        )
    }

    private suspend fun aviso() = prefs.goalEngineNoticePending.first()
    private suspend fun versaoVista() = prefs.lastSeenVersion.first()

    @Test
    fun `quem instala a app hoje nao recebe aviso nenhum`() = runTest(dispatcher) {
        // Sem perfil no primeiro arranque: o onboarding ainda nem começou.
        repository().onAppStart("1.0.0")

        assertFalse(aviso(), "explicou uma mudança a quem nunca viu a meta antiga")
        assertEquals("1.0.0", versaoVista())
    }

    @Test
    fun `quem ja usava a app e nunca teve versao registada recebe o aviso`() = runTest(dispatcher) {
        perfil()

        repository().onAppStart("1.0.0")

        assertTrue(aviso(), "deixou o número mudar sem explicação")
        assertEquals("1.0.0", versaoVista())
    }

    @Test
    fun `abrir a app outra vez na mesma versao nao repete nada`() = runTest(dispatcher) {
        perfil()
        val repo = repository()
        repo.onAppStart("1.0.0")
        repo.acknowledge()
        assertFalse(aviso())

        repo.onAppStart("1.0.0")

        assertFalse(aviso(), "o aviso voltou depois de lido")
    }

    @Test
    fun `atualizar de versao nao levanta o aviso outra vez`() = runTest(dispatcher) {
        perfil()
        val repo = repository()
        repo.onAppStart("1.0.0")
        repo.acknowledge()

        // Só a ausência de versão registada é sinal de instalação antiga. Uma atualização
        // normal não é motivo para repetir a conversa.
        repo.onAppStart("1.1.0")

        assertFalse(aviso())
        assertEquals("1.1.0", versaoVista())
    }

    @Test
    fun `sem aviso pendente nao se pergunta nada ao perfil`() = runTest(dispatcher) {
        perfil(exerciseAddBack = false)

        // O `onAppStart` nunca correu: não há marca levantada, e por isso não há mudança
        // a mostrar, mesmo com um perfil que a teria.
        assertNull(repository().pendingGoalChange())
    }

    @Test
    fun `com o exercicio ainda por somar ha mesmo mudanca a explicar`() = runTest(dispatcher) {
        perfil(exerciseAddBack = false)
        val repo = repository()
        repo.onAppStart("1.0.0")

        val mudanca = assertNotNull(repo.pendingGoalChange(), "não encontrou mudança nenhuma")
        assertTrue(GoalChangeReason.EXERCISE_ADD_BACK_FORCED_ON in mudanca.reasons)
        assertTrue(aviso(), "baixou a marca com uma mudança por mostrar")
    }

    @Test
    fun `sem mudanca real a marca baixa em silencio`() = runTest(dispatcher) {
        // Sedentário mantém o mesmo multiplicador de sempre e o exercício já soma: para
        // esta pessoa não mudou nada, e um aviso seria sobre coisa nenhuma.
        perfil(exerciseAddBack = true, activityLevel = ActivityLevel.SEDENTARY)
        val repo = repository()
        repo.onAppStart("1.0.0")

        assertNull(repo.pendingGoalChange())
        assertFalse(aviso(), "ficou uma marca levantada para um aviso que nunca aparece")
    }

    @Test
    fun `ler o aviso liga a soma do exercicio`() = runTest(dispatcher) {
        perfil(exerciseAddBack = false)
        val repo = repository()
        repo.onAppStart("1.0.0")

        repo.acknowledge()

        assertTrue(
            db.userProfileDao().get()?.exerciseAddBack == true,
            "o exercício continuou a não somar depois de explicado",
        )
        assertFalse(aviso())
    }

    @Test
    fun `a quem ja somava o exercicio nao se toca no perfil`() = runTest(dispatcher) {
        perfil(exerciseAddBack = true)
        val antes = assertNotNull(db.userProfileDao().get())
        val repo = repository()
        repo.onAppStart("1.0.0")

        repo.acknowledge()

        assertEquals(antes, db.userProfileDao().get(), "reescreveu um perfil que estava certo")
        assertFalse(aviso())
    }
}

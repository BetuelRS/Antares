package pt.antares.app.feature.profile.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.calc.BodyComposition
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.Sex
import pt.antares.app.core.model.WeightSource
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.profile.data.BodyMeasurementRepository
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A massa gorda é gravada em dois sítios ao mesmo tempo: no perfil, porque o basal a consulta a
 * cada cálculo, e no `body_measurement_log`, que guarda a série. Este teste existe para fixar o
 * que cada um dos dois fica a saber — incluindo onde discordam, que é o caso do "não sei".
 *
 * A aritmética das fórmulas não se repete aqui; vive no `BodyCompositionTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BodyCompositionSaveTest : ViewModelHarness() {

    private val hoje get() = todayEpochDay()

    private fun perfil(
        sex: Sex = Sex.MALE,
        bodyFatPct: Double? = null,
        bodyFatSource: BodyFatSource? = null,
    ) = UserProfileEntity(
        sex = sex,
        birthEpochDay = 10_000L,
        heightCm = 178,
        activityLevel = ActivityLevel.MODERATE,
        goalType = GoalType.MAINTAIN,
        goalRateKcal = 0,
        macroStrategy = MacroStrategy.BALANCED,
        customProteinG = null,
        customCarbsG = null,
        customFatG = null,
        bodyFatPct = bodyFatPct,
        bodyFatSource = bodyFatSource,
        updatedAt = 0L,
    )

    private fun measurements() = BodyMeasurementRepository(db.bodyMeasurementDao(), dispatcher)

    private suspend fun viewModelCom(
        profile: UserProfileEntity = perfil(),
        pesoKg: Double = 80.0,
    ): BodyCompositionViewModel {
        profileRepository().saveProfile(profile)
        db.weightLogDao().upsert(
            WeightLogEntity(
                id = "peso",
                epochDay = hoje,
                weightKg = pesoKg,
                note = null,
                source = WeightSource.MANUAL,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
        val vm = BodyCompositionViewModel(profileRepository(), measurements())
        vm.state.first { !it.loading && it.weightKg != null }
        return vm
    }

    private suspend fun perfilGravado() = db.userProfileDao().get()
    private suspend fun historicoDeHoje() = db.bodyMeasurementDao().latest()

    @Test
    fun `o ecra abre no metodo com que a massa gorda foi medida`() = runTest(dispatcher) {
        val vm = viewModelCom(perfil(bodyFatPct = 18.5, bodyFatSource = BodyFatSource.MEASURED))
        advanceUntilIdle()

        assertEquals(BodyFatMethod.KNOWN, vm.state.value.method)
        assertEquals("18,5", vm.state.value.knownPct, "o valor medido não voltou ao campo")
    }

    @Test
    fun `uma percentagem calculada pela Navy fica igual nos dois sitios`() = runTest(dispatcher) {
        val vm = viewModelCom()
        vm.setMethod(BodyFatMethod.MEASUREMENTS)
        vm.setWaist("88")
        vm.setNeck("38")
        advanceUntilIdle()

        val calculada = assertNotNull(vm.computedPct(), "a Navy não devolveu percentagem")
        vm.save()
        advanceUntilIdle()

        val p = assertNotNull(perfilGravado())
        val h = assertNotNull(historicoDeHoje(), "não ficou linha nenhuma no histórico")
        assertEquals(calculada, p.bodyFatPct)
        assertEquals(calculada, h.bodyFatPct, "o perfil e o histórico ficaram a discordar")
        assertEquals(BodyFatSource.NAVY, p.bodyFatSource)
        assertEquals(BodyFatSource.NAVY, h.bodyFatSource)
    }

    @Test
    fun `a virgula decimal e entendida como separador`() = runTest(dispatcher) {
        val vm = viewModelCom()
        vm.setMethod(BodyFatMethod.KNOWN)
        vm.setKnownPct("17,4")
        vm.setWaist("84,5")
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        assertEquals(17.4, perfilGravado()?.bodyFatPct)
        assertEquals(84.5, perfilGravado()?.waistCm)
    }

    @Test
    fun `medidas a meio nao inventam uma percentagem`() = runTest(dispatcher) {
        val vm = viewModelCom()
        vm.setMethod(BodyFatMethod.MEASUREMENTS)

        // Sem pescoço a Navy não tem conta possível. O que não pode acontecer é o ecrã
        // guardar zero, ou guardar a cintura com uma origem que ninguém calculou.
        vm.setWaist("88")
        advanceUntilIdle()

        assertNull(vm.computedPct())
        vm.save()
        advanceUntilIdle()

        val p = assertNotNull(perfilGravado())
        assertNull(p.bodyFatPct, "inventou uma percentagem sem medidas suficientes")
        assertNull(p.bodyFatSource, "gravou uma origem sem valor nenhum")
        assertEquals(88.0, p.waistCm, "a medida que a pessoa escreveu perdeu-se")
    }

    @Test
    fun `a mulher usa a anca, o homem ignora-a`() = runTest(dispatcher) {
        val elas = viewModelCom(perfil(sex = Sex.FEMALE))
        elas.setMethod(BodyFatMethod.MEASUREMENTS)
        elas.setWaist("80")
        elas.setNeck("32")
        advanceUntilIdle()
        assertNull(elas.computedPct(), "calculou sem a anca")

        elas.setHip("98")
        advanceUntilIdle()
        assertNotNull(elas.computedPct(), "com a anca continuou sem calcular")
    }

    @Test
    fun `pelo IMC a origem gravada diz que foi estimada`() = runTest(dispatcher) {
        val vm = viewModelCom(pesoKg = 80.0)
        vm.setMethod(BodyFatMethod.BMI)
        advanceUntilIdle()

        val esperada = assertNotNull(
            BodyComposition.bmi(80.0, 178)?.let {
                BodyComposition.deurenbergBodyFat(
                    sex = Sex.MALE,
                    bmi = it,
                    ageYears = pt.antares.app.core.calc.NutritionCalc.ageYears(10_000L, hoje),
                )
            },
        )
        assertEquals(esperada, vm.computedPct())

        vm.save()
        advanceUntilIdle()
        assertEquals(BodyFatSource.BMI, perfilGravado()?.bodyFatSource)
    }

    @Test
    fun `braco, coxa e peito so existem no historico`() = runTest(dispatcher) {
        val vm = viewModelCom()
        vm.setMethod(BodyFatMethod.KNOWN)
        vm.setKnownPct("20")
        vm.setArm("36")
        vm.setThigh("58")
        vm.setChest("102")
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        val h = assertNotNull(historicoDeHoje())
        assertEquals(36.0, h.armCm)
        assertEquals(58.0, h.thighCm)
        assertEquals(102.0, h.chestCm)
    }

    @Test
    fun `escolher nao sei limpa o perfil, mas o historico do dia guarda o que ja la estava`() =
        runTest(dispatcher) {
            val vm = viewModelCom()
            vm.setMethod(BodyFatMethod.KNOWN)
            vm.setKnownPct("22")
            advanceUntilIdle()
            vm.save()
            advanceUntilIdle()
            assertEquals(22.0, historicoDeHoje()?.bodyFatPct)

            vm.setMethod(BodyFatMethod.NONE)
            advanceUntilIdle()
            vm.save()
            advanceUntilIdle()

            // O perfil esquece, porque é ele que alimenta o basal e não pode ficar com um
            // valor que a pessoa retirou.
            assertNull(perfilGravado()?.bodyFatPct, "o perfil ficou com a massa gorda retirada")
            assertNull(perfilGravado()?.bodyFatSource)

            // O histórico não: o `record` funde com o que lá está, e nada apaga uma medição
            // do próprio dia. As duas fontes ficam a discordar até à medição seguinte.
            assertEquals(
                22.0,
                historicoDeHoje()?.bodyFatPct,
                "o histórico apagou uma medição em vez de a manter",
            )
        }

    @Test
    fun `medir outra vez no mesmo dia reescreve a linha, e nao acrescenta outra`() =
        runTest(dispatcher) {
            val vm = viewModelCom()
            vm.setMethod(BodyFatMethod.KNOWN)
            vm.setKnownPct("20")
            advanceUntilIdle()
            vm.save()
            advanceUntilIdle()

            vm.setKnownPct("19")
            advanceUntilIdle()
            vm.save()
            advanceUntilIdle()

            val linhas = db.bodyMeasurementDao().observeAll().first()
            assertEquals(1, linhas.size, "o mesmo dia ficou com ${linhas.size} linhas")
            assertEquals(19.0, linhas.first().bodyFatPct)
            assertTrue(linhas.first().epochDay == hoje)
        }
}

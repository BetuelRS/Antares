package pt.antares.app.feature.diary

import android.content.ContentProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.calc.AguaDaComida
import pt.antares.app.core.calc.Targets
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.diary_water_food_unknown
import kotlin.test.Test

/**
 * A água no resumo do dia, nos três resultados do `AguaDaComida`.
 *
 * A barra soma a da comida só quando ela se sabe. Quando se comeu e menos de metade trouxe teor
 * de água medido, a barra mostrava a bebida sozinha como se fosse a água toda — o cartão do fim
 * dizia que não se sabia, e o resumo, que é o que se lê, calava-se.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ResumoDoDiaUiTest {

    // O cartão lê as cadeias por `stringResource`, e isso só funciona com o `ContentProvider`
    // dos recursos gerados já arrancado — molde do `FluxoUiHarness`.
    @Before
    fun arrancaOsRecursos() {
        @Suppress("UNCHECKED_CAST")
        val provider = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
            as Class<ContentProvider>
        Robolectric.buildContentProvider(provider).create()
    }

    private val estado = DiaryState(
        epochDay = 20_000,
        isToday = true,
        loading = false,
        targets = Targets(kcal = 2000, proteinG = 120, carbsG = 220, fatG = 70),
        waterMl = 500,
        waterGoalMl = 2500,
    )

    private fun frase() = runBlocking { getString(Res.string.diary_water_food_unknown) }

    @Test
    fun `sem cobertura o resumo diz que falta a agua da comida`() = runComposeUiTest {
        setContent { DaySummaryCard(estado, AguaDaComida.Resultado.SemCobertura) }
        waitForIdle()

        onNodeWithText(frase()).assertExists()
    }

    @Test
    fun `com a agua medida ou sem registo nao ha nada a dizer`() = runComposeUiTest {
        var resultado by mutableStateOf<AguaDaComida.Resultado>(AguaDaComida.Resultado.Medida(300))
        setContent { DaySummaryCard(estado, resultado) }
        waitForIdle()
        onNodeWithText(frase()).assertDoesNotExist()

        // Sem nada comido, a bebida é mesmo a água toda: não há parcela por medir.
        resultado = AguaDaComida.Resultado.SemRegisto
        waitForIdle()
        onNodeWithText(frase()).assertDoesNotExist()
    }
}

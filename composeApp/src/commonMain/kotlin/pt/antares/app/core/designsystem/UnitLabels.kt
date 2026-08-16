package pt.antares.app.core.designsystem

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.UnitConversions
import kotlin.math.roundToInt
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.common_floz
import pt.antares.app.generated.resources.common_grams_short
import pt.antares.app.generated.resources.common_kg
import pt.antares.app.generated.resources.common_lb
import pt.antares.app.generated.resources.common_ml
import pt.antares.app.generated.resources.common_oz
import pt.antares.app.generated.resources.run_pace_unit
import pt.antares.app.generated.resources.run_pace_unit_mi
import pt.antares.app.generated.resources.run_unit_km
import pt.antares.app.generated.resources.run_unit_mi

/**
 * O rótulo da unidade sai daqui e de mais lado nenhum.
 *
 * A app tinha o sistema imperial no perfil, no arranque, no progresso e no peso — e o treino
 * continuava em quilos, a corrida em quilómetros e as porções em gramas. Um rótulo escrito à
 * mão num ecrã é exatamente como isso acontece: ninguém o vê ao mudar a preferência.
 */
fun weightUnitLabel(system: UnitSystem): StringResource =
    if (system == UnitSystem.IMPERIAL) Res.string.common_lb else Res.string.common_kg

fun distanceUnitLabel(system: UnitSystem): StringResource =
    if (system == UnitSystem.IMPERIAL) Res.string.run_unit_mi else Res.string.run_unit_km

fun paceUnitLabel(system: UnitSystem): StringResource =
    if (system == UnitSystem.IMPERIAL) Res.string.run_pace_unit_mi else Res.string.run_pace_unit

/** A unidade de uma porção: sólida em gramas ou onças, líquida em mililitros ou onças líquidas. */
fun portionUnitLabel(system: UnitSystem, liquid: Boolean): StringResource = when {
    system != UnitSystem.IMPERIAL -> if (liquid) Res.string.common_ml else Res.string.common_grams_short
    liquid -> Res.string.common_floz
    else -> Res.string.common_oz
}

/**
 * Um peso com a unidade, arredondado ao inteiro. A base guarda sempre quilos: aqui converte-se
 * para ver, e é o único sítio onde isso acontece nos ecrãs de treino.
 */
@Composable
fun weightWithUnit(kg: Double, system: UnitSystem): String =
    "${UnitConversions.weightToDisplay(kg, system).roundToInt()} ${stringResource(weightUnitLabel(system))}"

/**
 * Como [weightWithUnit], mas com uma casa decimal — a precisão a que uma pesagem se lê. Meio
 * quilo é a diferença que faz alguém achar que a semana correu bem ou mal.
 */
@Composable
fun bodyWeightWithUnit(kg: Double, system: UnitSystem): String =
    "${fmtG(UnitConversions.weightToDisplay(kg, system))} ${stringResource(weightUnitLabel(system))}"

/**
 * Uma porção com a unidade. Em imperial leva uma casa decimal: uma onça são quase trinta
 * gramas, e ao inteiro uma refeição inteira mudava de tamanho no arredondamento.
 */
@Composable
fun porcaoComUnidade(quantidade: Double, liquido: Boolean): String {
    val system = rememberUnitSystem()
    val valor = UnitConversions.portionToDisplay(quantidade, system, liquido)
    val numero = if (system == UnitSystem.IMPERIAL) {
        ((valor * UMA_CASA).roundToInt() / UMA_CASA.toDouble()).toString()
    } else {
        valor.roundToInt().toString()
    }
    return "$numero ${stringResource(portionUnitLabel(system, liquido))}"
}

// Uma casa decimal, que é o que uma onça precisa e uma grama não.
private const val UMA_CASA = 10

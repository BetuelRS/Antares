package pt.antares.app.core.designsystem

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.UnitConversions
import kotlin.math.roundToInt
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.common_cm
import pt.antares.app.generated.resources.common_floz
import pt.antares.app.generated.resources.common_ft
import pt.antares.app.generated.resources.common_ft_short
import pt.antares.app.generated.resources.common_m
import pt.antares.app.generated.resources.common_in
import pt.antares.app.generated.resources.common_grams_short
import pt.antares.app.generated.resources.common_kg
import pt.antares.app.generated.resources.common_lb
import pt.antares.app.generated.resources.common_ml
import pt.antares.app.generated.resources.common_oz
import pt.antares.app.generated.resources.profile_health_kg_per_week
import pt.antares.app.generated.resources.profile_health_lb_per_week
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

/** Metros ou pés, para o desnível de uma corrida. */
fun elevationUnitLabel(system: UnitSystem): StringResource =
    if (system == UnitSystem.IMPERIAL) Res.string.common_ft_short else Res.string.common_m

/**
 * Um peso com a unidade, arredondado ao inteiro. A base guarda sempre quilos: aqui converte-se
 * para ver, e é o único sítio onde isso acontece nos ecrãs de treino.
 *
 * É o formatador dos **volumes** — somas de milhares, onde uma casa decimal não diz nada.
 * Para a carga de uma série, [loadWithUnit].
 */
@Composable
fun weightWithUnit(kg: Double, system: UnitSystem): String =
    "${UnitConversions.weightToDisplay(kg, system).roundToInt()} ${stringResource(weightUnitLabel(system))}"

/**
 * A carga de uma série, com a casa decimal quando ela existe. **62,5 kg é um disco a sério**,
 * e arredondar punha o ecrã a dizer 63 sobre uma série que a pessoa gravou a 62,5 — a mesma
 * série lia-se de duas maneiras conforme se olhasse para a linha ou para a correção.
 *
 * Os inteiros continuam a ler-se inteiros: o [trimmedDecimal] corta o zero à direita.
 */
@Composable
fun loadWithUnit(kg: Double, system: UnitSystem): String =
    trimmedDecimal(UnitConversions.weightToDisplay(kg, system), comma = virgulaDecimal()) +
        " " + stringResource(weightUnitLabel(system))

/**
 * Como [weightWithUnit], mas com uma casa decimal — a precisão a que uma pesagem se lê. Meio
 * quilo é a diferença que faz alguém achar que a semana correu bem ou mal.
 */
@Composable
fun bodyWeightWithUnit(kg: Double, system: UnitSystem): String =
    "${fmtG(UnitConversions.weightToDisplay(kg, system))} ${stringResource(weightUnitLabel(system))}"

/**
 * Um comprimento do corpo com a unidade: centímetros ou polegadas.
 */
@Composable
fun lengthWithUnit(cm: Double, system: UnitSystem): String =
    "${fmtG(UnitConversions.lengthToDisplay(cm, system))} ${stringResource(lengthUnitLabel(system))}"

fun lengthUnitLabel(system: UnitSystem): StringResource =
    if (system == UnitSystem.IMPERIAL) Res.string.common_in else Res.string.common_cm

/**
 * Um ritmo de peso por semana, com a unidade. O número converte-se como qualquer peso — meio
 * quilo por semana é uma libra e um bocado —, e sem isso o cartão do corpo mostrava «153,9 lb»
 * por cima de «0,4 kg/semana», que é a mesma pessoa medida em duas escalas.
 */
@Composable
fun ratePerWeekWithUnit(kgPerWeek: Double, system: UnitSystem): String {
    val rotulo = if (system == UnitSystem.IMPERIAL) {
        Res.string.profile_health_lb_per_week
    } else {
        Res.string.profile_health_kg_per_week
    }
    return "${fmtG(UnitConversions.weightToDisplay(kgPerWeek, system))} ${stringResource(rotulo)}"
}

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

/**
 * A altura com a unidade. Em imperial vai em pés e polegadas — «5 ft 10 in» —, e não em
 * polegadas soltas: ninguém diz a altura em 70 polegadas.
 */
@Composable
fun heightWithUnit(cm: Int, system: UnitSystem): String =
    if (system == UnitSystem.IMPERIAL) {
        val (pes, polegadas) = UnitConversions.cmToFtIn(cm)
        "$pes ${stringResource(Res.string.common_ft)} " +
            "$polegadas ${stringResource(Res.string.common_in)}"
    } else {
        "$cm ${stringResource(Res.string.common_cm)}"
    }

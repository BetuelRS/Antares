package pt.antares.app.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Formatação de números à mão, sem o formatador do sistema, que não existe no código comum
 * do Kotlin. Só há uma decisão de idioma: a vírgula decimal em português.
 *
 * Os pares `@Composable` leem o idioma da composição; as funções puras recebem-no por
 * parâmetro, e são essas que os testes usam.
 */
@Composable
fun fmtG(value: Double): String = oneDecimal(value, comma = Locale.current.language == "pt")

@Composable
fun fmt2(value: Double): String = twoDecimals(value, comma = Locale.current.language == "pt")

/**
 * Se um idioma escreve os decimais com vírgula.
 *
 * Recebe o idioma em vez de o ir buscar, porque **nem tudo o que a app escreve nasce dentro
 * de uma composição**: a notificação da corrida vive num serviço, e foi por não ter onde
 * perguntar isto que ficou com a vírgula escrita à mão — o ecrã dizia `0.06 km` e a
 * notificação `0,05 km` na mesma corrida.
 */
fun usaVirgulaDecimal(idioma: String): Boolean = idioma == "pt"

/**
 * A mesma regra, para quem está numa composição e tem o idioma à mão.
 */
@Composable
fun virgulaDecimal(): Boolean = usaVirgulaDecimal(Locale.current.language)

fun twoDecimals(value: Double, comma: Boolean): String = fixedDecimals(value, 2, comma)

fun fixedDecimals(value: Double, places: Int, comma: Boolean): String {
    var scale = 1L
    repeat(places) { scale *= 10 }
    val rounded = (value * scale).roundToLong() / scale.toDouble()
    val intPart = rounded.toLong()
    val dec = (abs(rounded - intPart) * scale).roundToLong()

    // O sinal tem de ser reposto à mão para valores entre -1 e 0: a parte inteira é zero e
    // um zero não guarda sinal, e "-0,4" saía como "0,4".
    val sign = if (rounded < 0 && intPart == 0L) "-" else ""
    val text = if (places == 0) {
        "$sign$intPart"
    } else {
        "$sign$intPart." + dec.toString().padStart(places, '0')
    }
    return if (comma) text.replace('.', ',') else text
}

@Composable
fun fmtT(value: Double, places: Int = 1): String =
    trimmedDecimal(value, places, comma = Locale.current.language == "pt")

/** Corta os zeros à direita: 70,0 fica 70, mas 70,5 fica como está. */
fun trimmedDecimal(value: Double, places: Int = 1, comma: Boolean): String {
    var text = fixedDecimals(value, places, comma)
    val sep = if (comma) ',' else '.'
    if (text.contains(sep)) {
        text = text.trimEnd('0').trimEnd(sep)
    }
    return text
}

fun oneDecimal(value: Double, comma: Boolean): String = fixedDecimals(value, 1, comma)

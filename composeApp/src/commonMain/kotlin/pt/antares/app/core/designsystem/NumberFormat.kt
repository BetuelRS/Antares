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
 * Se o idioma em uso escreve os decimais com vírgula. Existe para quem formata fora daqui
 * poder seguir a mesma regra em vez de a escolher à mão — foi assim que a corrida ficou com
 * vírgula fixa e passou a discordar do peso no mesmo cartão.
 */
@Composable
fun virgulaDecimal(): Boolean = Locale.current.language == "pt"

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

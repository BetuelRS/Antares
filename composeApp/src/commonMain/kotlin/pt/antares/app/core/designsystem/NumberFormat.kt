package pt.antares.app.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun fmtG(value: Double): String = oneDecimal(value, comma = Locale.current.language == "pt")

@Composable
fun fmt2(value: Double): String = twoDecimals(value, comma = Locale.current.language == "pt")

fun twoDecimals(value: Double, comma: Boolean): String = fixedDecimals(value, 2, comma)

fun fixedDecimals(value: Double, places: Int, comma: Boolean): String {
    var scale = 1L
    repeat(places) { scale *= 10 }
    val rounded = (value * scale).roundToLong() / scale.toDouble()
    val intPart = rounded.toLong()
    val dec = (abs(rounded - intPart) * scale).roundToLong()

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

fun trimmedDecimal(value: Double, places: Int = 1, comma: Boolean): String {
    var text = fixedDecimals(value, places, comma)
    val sep = if (comma) ',' else '.'
    if (text.contains(sep)) {
        text = text.trimEnd('0').trimEnd(sep)
    }
    return text
}

fun oneDecimal(value: Double, comma: Boolean): String = fixedDecimals(value, 1, comma)

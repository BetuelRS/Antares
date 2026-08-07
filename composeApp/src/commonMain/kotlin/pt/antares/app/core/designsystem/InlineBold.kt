package pt.antares.app.core.designsystem

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

fun inlineBold(texto: String): AnnotatedString = buildAnnotatedStringFrom(texto)

private const val MARCA = "**"

private fun buildAnnotatedStringFrom(texto: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    var negrito = false

    while (i < texto.length) {
        val proxima = texto.indexOf(MARCA, i)
        if (proxima < 0) {

            builder.appendComEstilo(texto.substring(i), negrito)
            break
        }
        builder.appendComEstilo(texto.substring(i, proxima), negrito)
        negrito = !negrito
        i = proxima + MARCA.length
    }
    return builder.toAnnotatedString()
}

private fun AnnotatedString.Builder.appendComEstilo(trecho: String, negrito: Boolean) {
    if (trecho.isEmpty()) return
    if (!negrito) {
        append(trecho)
        return
    }
    val inicio = length
    append(trecho)
    addStyle(SpanStyle(fontWeight = FontWeight.Bold), inicio, length)
}

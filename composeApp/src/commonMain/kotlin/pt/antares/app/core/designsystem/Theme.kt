package pt.antares.app.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * O esquema escuro, **inteiro**.
 *
 * O que aqui estava nomeava nove cores e deixava as outras trinta ao Material — e as que
 * ficavam por dizer são exactamente as que mais se vêem: o `Card` do Material não pinta com
 * `surface`, pinta com um dos `surfaceContainer`, e esses vinham do lavanda-cinzento do
 * baseline. **Era essa a razão de a app parecer cinzenta ao lado dos esboços**, com a mesma
 * paleta declarada nos dois sítios.
 *
 * O `surfaceTint` transparente é a segunda metade da mesma história: o Material aclara uma
 * superfície elevada com um véu da primária, e um cartão sobre fundo quase preto ficava
 * rosado sem ninguém ter pedido isso. A elevação nesta app lê-se pelo contorno, que é o que
 * os esboços desenham.
 */
private val DarkColors = darkColorScheme(
    background = AntaresColors.backgroundDark,
    onBackground = AntaresColors.inkDark,

    surface = AntaresColors.surfaceDark,
    onSurface = AntaresColors.inkDark,
    surfaceVariant = AntaresColors.surfaceVariantDark,
    onSurfaceVariant = AntaresColors.inkDimDark,

    // A escada dos contentores, do mais fundo ao mais alto. É daqui que saem os cartões, as
    // folhas, os menus e os campos — e era daqui que saía o cinzento.
    surfaceContainerLowest = AntaresColors.groundDark,
    surfaceContainerLow = AntaresColors.backgroundDark,
    surfaceContainer = AntaresColors.surfaceDark,
    surfaceContainerHigh = AntaresColors.surfaceVariantDark,
    surfaceContainerHighest = AntaresColors.surfaceVariantDark,
    surfaceBright = AntaresColors.surfaceVariantDark,
    surfaceDim = AntaresColors.groundDark,
    inverseSurface = AntaresColors.inkDark,
    inverseOnSurface = AntaresColors.backgroundDark,
    surfaceTint = Color.Transparent,

    primary = AntaresColors.primaryDark,
    onPrimary = AntaresColors.backgroundDark,
    primaryContainer = AntaresColors.primaryContainerDark,
    onPrimaryContainer = AntaresColors.primaryDark,
    inversePrimary = AntaresColors.primaryDark,

    secondary = AntaresColors.secondaryDark,
    onSecondary = AntaresColors.backgroundDark,
    secondaryContainer = AntaresColors.secondaryContainerDark,
    onSecondaryContainer = AntaresColors.secondaryDark,

    tertiary = AntaresColors.tertiaryDark,
    onTertiary = AntaresColors.backgroundDark,
    tertiaryContainer = AntaresColors.tertiaryContainerDark,
    onTertiaryContainer = AntaresColors.tertiaryDark,

    error = AntaresColors.errorDark,
    onError = AntaresColors.backgroundDark,
    errorContainer = AntaresColors.errorContainerDark,
    onErrorContainer = AntaresColors.errorDark,

    outline = AntaresColors.outlineDark,
    outlineVariant = AntaresColors.outlineSoftDark,
    scrim = AntaresColors.groundDark,
)

/** O mesmo, do lado claro: papel quente, tinta quente, e nenhuma cor deixada por dizer. */
private val LightColors = lightColorScheme(
    background = AntaresColors.backgroundLight,
    onBackground = AntaresColors.inkLight,

    surface = AntaresColors.surfaceLight,
    onSurface = AntaresColors.inkLight,
    surfaceVariant = AntaresColors.surfaceVariantLight,
    onSurfaceVariant = AntaresColors.inkDimLight,

    surfaceContainerLowest = AntaresColors.surfaceLight,
    surfaceContainerLow = AntaresColors.backgroundLight,
    surfaceContainer = AntaresColors.surfaceLight,
    surfaceContainerHigh = AntaresColors.surfaceVariantLight,
    surfaceContainerHighest = AntaresColors.surfaceVariantLight,
    surfaceBright = AntaresColors.surfaceLight,
    surfaceDim = AntaresColors.groundLight,
    inverseSurface = AntaresColors.inkLight,
    inverseOnSurface = AntaresColors.backgroundLight,
    surfaceTint = Color.Transparent,

    primary = AntaresColors.primaryLight,
    onPrimary = AntaresColors.surfaceLight,
    primaryContainer = AntaresColors.primaryContainerLight,
    onPrimaryContainer = AntaresColors.primaryLight,
    inversePrimary = AntaresColors.primaryLight,

    secondary = AntaresColors.secondaryLight,
    onSecondary = AntaresColors.surfaceLight,
    secondaryContainer = AntaresColors.secondaryContainerLight,
    onSecondaryContainer = AntaresColors.secondaryLight,

    tertiary = AntaresColors.tertiaryLight,
    onTertiary = AntaresColors.surfaceLight,
    tertiaryContainer = AntaresColors.tertiaryContainerLight,
    onTertiaryContainer = AntaresColors.tertiaryLight,

    error = AntaresColors.errorLight,
    onError = AntaresColors.surfaceLight,
    errorContainer = AntaresColors.errorContainerLight,
    onErrorContainer = AntaresColors.errorLight,

    outline = AntaresColors.outlineLight,
    outlineVariant = AntaresColors.outlineSoftLight,
    scrim = AntaresColors.inkLight,
)

// O Material não tem cor de sucesso, e usar a primária para isso confundia-a com uma ação.
// Viaja à parte do esquema, pela composição.
@Immutable
data class AntaresExtraColors(val success: Color)

val LocalAntaresExtraColors = staticCompositionLocalOf {
    AntaresExtraColors(success = AntaresColors.successDark)
}

val MaterialTheme.success: Color
    @Composable get() = LocalAntaresExtraColors.current.success

enum class ThemeMode { SYSTEM, LIGHT, DARK }

// Duas apresentações do resumo do dia, à escolha nas definições. Não muda dados nenhuns.

/**
 * Recebe `darkTheme` já resolvido em vez de ler a preferência: o tema pode ser forçado nas
 * definições, e é acima daqui que a escolha da pessoa vence a do sistema.
 */
@Composable
fun AntaresTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extras = AntaresExtraColors(
        success = if (darkTheme) AntaresColors.successDark else AntaresColors.successLight,
    )
    CompositionLocalProvider(LocalAntaresExtraColors provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = antaresTypography(),
            shapes = AntaresShapes,
            content = content,
        )
    }
}

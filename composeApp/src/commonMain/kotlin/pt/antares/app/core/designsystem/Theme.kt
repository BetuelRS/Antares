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

private val DarkColors = darkColorScheme(
    background = AntaresColors.backgroundDark,
    surface = AntaresColors.surfaceDark,
    surfaceVariant = AntaresColors.surfaceVariantDark,
    primary = AntaresColors.primaryDark,
    onPrimary = AntaresColors.backgroundDark,
    secondary = AntaresColors.secondaryDark,
    tertiary = AntaresColors.tertiaryDark,
    error = AntaresColors.errorDark,
    outline = AntaresColors.outlineDark,
)

private val LightColors = lightColorScheme(
    background = AntaresColors.backgroundLight,
    surface = AntaresColors.surfaceLight,
    surfaceVariant = AntaresColors.surfaceVariantLight,
    primary = AntaresColors.primaryLight,
    onPrimary = AntaresColors.surfaceLight,
    secondary = AntaresColors.secondaryLight,
    tertiary = AntaresColors.tertiaryLight,
    error = AntaresColors.errorLight,
    outline = AntaresColors.outlineLight,
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
enum class HeroStyle { CLASSIC, RING }

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

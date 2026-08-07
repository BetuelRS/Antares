package pt.antares.app.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.inter_variable
import pt.antares.app.generated.resources.space_grotesk_bold
import pt.antares.app.generated.resources.space_grotesk_medium
import pt.antares.app.generated.resources.space_grotesk_regular

@Composable
fun antaresDisplayFontFamily(): FontFamily = FontFamily(
    Font(Res.font.space_grotesk_regular, weight = FontWeight.Normal),
    Font(Res.font.space_grotesk_medium, weight = FontWeight.Medium),
    Font(Res.font.space_grotesk_bold, weight = FontWeight.Bold),
)

@Composable
fun antaresBodyFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_variable, weight = FontWeight.Normal),
)

@Composable
fun antaresTypography(): Typography {
    val display = antaresDisplayFontFamily()
    val body = antaresBodyFontFamily()
    return Typography(
        displayLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 40.sp),
        headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 26.sp),
        titleLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 16.sp),
        bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 11.sp),
    )
}

val TabularNumbersFeature = "tnum"

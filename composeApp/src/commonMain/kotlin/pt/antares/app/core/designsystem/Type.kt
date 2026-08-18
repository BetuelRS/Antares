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
    // Os quinze estilos do Material 3 são declarados todos, e não só os que a app usa
    // hoje: o que ficar de fora cai no Roboto do sistema sem erro nenhum, e a diferença
    // só se vê no telemóvel. O `TipografiaCompletaTest` é quem o impede.
    //
    // A escala é a da app e não a do Material: o `displayLarge` são 40 sp e não 57. Os
    // tamanhos que faltavam foram postos a completar as rampas existentes, sem mexer nos
    // sete que já estavam.
    return Typography(
        displayLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 40.sp),
        displayMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 34.sp),
        displaySmall = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 30.sp),
        headlineLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 28.sp),
        headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 26.sp),
        headlineSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 22.sp),
        titleLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        titleSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 16.sp),
        bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 12.sp),

        // Os rótulos ficam no corpo e a Medium: são texto pequeno e maiúsculo, onde o
        // peso normal da Inter se lê mal.
        labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 12.sp),
        labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 11.sp),
    )
}

val TabularNumbersFeature = "tnum"

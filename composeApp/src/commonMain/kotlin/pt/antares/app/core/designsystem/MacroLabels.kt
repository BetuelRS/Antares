package pt.antares.app.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.macro_c
import pt.antares.app.generated.resources.macro_f
import pt.antares.app.generated.resources.macro_p

@Immutable
data class MacroInitials(val p: String, val c: String, val f: String)

@Composable
fun macroInitials(): MacroInitials = MacroInitials(
    p = stringResource(Res.string.macro_p),
    c = stringResource(Res.string.macro_c),
    f = stringResource(Res.string.macro_f),
)

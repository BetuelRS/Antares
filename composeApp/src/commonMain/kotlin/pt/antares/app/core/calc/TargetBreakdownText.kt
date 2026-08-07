package pt.antares.app.core.calc

import pt.antares.app.core.designsystem.fixedDecimals
import pt.antares.app.core.designsystem.trimmedDecimal
import kotlin.math.abs
import kotlin.math.roundToInt

object TargetBreakdownText {

    fun args(step: TargetBreakdown.Step, comma: Boolean): List<String> = when (step.kind) {
        TargetBreakdown.Kind.BMR_FROM_LEAN -> {

            val lean = step.values[0]
            val perKg = if (lean > 0) (step.exact - 370.0) / lean else 0.0
            val cunningham = abs(perKg - 22.0) < abs(perKg - 21.6)
            listOf(
                trimmedDecimal(lean, comma = comma),
                if (cunningham) "22" else fixedDecimals(21.6, 1, comma),
                if (cunningham) "500" else "370",
                trimmedDecimal(step.exact, comma = comma),
            )
        }

        TargetBreakdown.Kind.BMR_MIFFLIN -> {

            val sexo = step.values[3]
            listOf(
                trimmedDecimal(10 * step.values[0], comma = comma),
                trimmedDecimal(6.25 * step.values[1], comma = comma),
                trimmedDecimal(5 * step.values[2], comma = comma),
                signedTerm(sexo, comma),
                trimmedDecimal(step.exact, comma = comma),
            )
        }

        TargetBreakdown.Kind.ACTIVITY -> listOf(
            trimmedDecimal(step.values[0], comma = comma),
            fixedDecimals(step.values[1], 2, comma),
            step.result.toString(),
        )

        TargetBreakdown.Kind.RATE -> listOf(
            step.values[0].roundToInt().toString(),
            signedTerm(step.values[1], comma),
            step.result.toString(),
        )

        TargetBreakdown.Kind.FLOOR -> emptyList()
    }

    private fun signedTerm(value: Double, comma: Boolean): String {
        val text = trimmedDecimal(abs(value), comma = comma)
        return if (value >= 0) "+ $text" else "− $text"
    }
}

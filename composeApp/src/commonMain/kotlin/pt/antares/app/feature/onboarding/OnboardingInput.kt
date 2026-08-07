package pt.antares.app.feature.onboarding

import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.UnitConversions

object OnboardingInput {

    val HEIGHT_CM_RANGE = 100..250

    val WEIGHT_KG_RANGE = 30.0..300.0

    fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    fun heightCm(unit: UnitSystem, cmText: String, feetText: String, inchesText: String): Int? {
        if (unit == UnitSystem.METRIC) {
            return cmText.trim().toIntOrNull()?.takeIf { it in HEIGHT_CM_RANGE }
        }
        val feet = feetText.trim().toIntOrNull() ?: return null
        val inches = inchesText.trim().ifBlank { "0" }.toIntOrNull() ?: return null

        if (inches !in 0..11 || feet < 0) return null
        return UnitConversions.ftInToCm(feet, inches).takeIf { it in HEIGHT_CM_RANGE }
    }

    fun weightKg(unit: UnitSystem, text: String): Double? {
        val valor = parseDecimal(text) ?: return null
        val kg = if (unit == UnitSystem.IMPERIAL) UnitConversions.lbToKg(valor) else valor
        return kg.takeIf { it in WEIGHT_KG_RANGE }
    }

    fun goalWeightKg(unit: UnitSystem, text: String): Double? =
        if (text.isBlank()) null else weightKg(unit, text)

    fun goalWeightAcceptable(unit: UnitSystem, text: String): Boolean =
        text.isBlank() || goalWeightKg(unit, text) != null

    fun goalContradictsDirection(
        losing: Boolean,
        currentKg: Double?,
        goalKg: Double?,
    ): Boolean {
        if (currentKg == null || goalKg == null) return false
        val delta = goalKg - currentKg
        if (kotlin.math.abs(delta) < CONTRADICTION_MARGIN_KG) return false
        return if (losing) delta > 0 else delta < 0
    }

    const val CONTRADICTION_MARGIN_KG = 0.5
}

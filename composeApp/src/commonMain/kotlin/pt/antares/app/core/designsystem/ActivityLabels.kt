package pt.antares.app.core.designsystem

import org.jetbrains.compose.resources.StringResource
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.onb_activity_athlete
import pt.antares.app.generated.resources.onb_activity_high
import pt.antares.app.generated.resources.onb_activity_light
import pt.antares.app.generated.resources.onb_activity_moderate
import pt.antares.app.generated.resources.onb_activity_sedentary

fun activityLevelLabel(level: ActivityLevel): StringResource = when (level) {
    ActivityLevel.SEDENTARY -> Res.string.onb_activity_sedentary
    ActivityLevel.LIGHT -> Res.string.onb_activity_light
    ActivityLevel.MODERATE -> Res.string.onb_activity_moderate
    ActivityLevel.HIGH -> Res.string.onb_activity_high
    ActivityLevel.ATHLETE -> Res.string.onb_activity_athlete
}

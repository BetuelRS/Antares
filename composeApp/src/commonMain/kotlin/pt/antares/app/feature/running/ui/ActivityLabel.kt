package pt.antares.app.feature.running.ui

import org.jetbrains.compose.resources.StringResource
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.run_type_ride
import pt.antares.app.generated.resources.run_type_run
import pt.antares.app.generated.resources.run_type_walk

fun activityLabel(type: ActivityType): StringResource = when (type) {
    ActivityType.RUN -> Res.string.run_type_run
    ActivityType.WALK -> Res.string.run_type_walk
    ActivityType.RIDE -> Res.string.run_type_ride
}

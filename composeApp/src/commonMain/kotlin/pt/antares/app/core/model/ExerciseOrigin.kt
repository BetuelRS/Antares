package pt.antares.app.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ExerciseOrigin { MANUAL, WORKOUT, RUN, HEALTH_CONNECT }

@Serializable
enum class WeightSource { MANUAL, HEALTH_CONNECT }

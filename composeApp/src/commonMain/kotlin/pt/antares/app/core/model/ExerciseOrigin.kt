package pt.antares.app.core.model

import kotlinx.serialization.Serializable

// Quem criou a linha de calorias do dia. As três últimas são geradas pela app a partir de
// outra coisa, e é o que permite mantê-las em dia quando a origem muda ou é apagada.
@Serializable
enum class ExerciseOrigin { MANUAL, WORKOUT, RUN, HEALTH_CONNECT }

@Serializable
enum class WeightSource { MANUAL, HEALTH_CONNECT }

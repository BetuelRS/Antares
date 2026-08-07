package pt.antares.app.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class SessionStatus { ACTIVE, DONE, DISCARDED }

@Serializable
enum class FastingStatus { ACTIVE, COMPLETED, BROKEN }

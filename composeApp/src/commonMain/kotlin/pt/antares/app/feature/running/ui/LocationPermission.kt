package pt.antares.app.feature.running.ui

import androidx.compose.runtime.Composable

enum class LocationPermissionStatus { NOT_REQUESTED, GRANTED, DENIED }

interface LocationPermissionController {
    val status: LocationPermissionStatus
    fun request()
}

@Composable
expect fun rememberLocationPermission(): LocationPermissionController

@Composable
expect fun rememberOpenAppSettings(): () -> Unit

@Composable
expect fun rememberLocationServicesEnabled(): Boolean

@Composable
expect fun rememberOpenLocationSettings(): () -> Unit

package pt.antares.app.feature.workout.ui

import androidx.compose.runtime.Composable

@Composable
expect fun rememberNotificationPermissionRequester(): () -> Unit

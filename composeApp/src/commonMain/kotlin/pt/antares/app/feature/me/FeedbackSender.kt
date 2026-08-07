package pt.antares.app.feature.me

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFeedbackSender(): () -> Unit

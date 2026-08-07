package pt.antares.app.core.util

import androidx.compose.runtime.Composable

expect class VoiceInputController {
    val available: Boolean

    fun start(prompt: String)
}

@Composable
expect fun rememberVoiceInput(onText: (String) -> Unit): VoiceInputController

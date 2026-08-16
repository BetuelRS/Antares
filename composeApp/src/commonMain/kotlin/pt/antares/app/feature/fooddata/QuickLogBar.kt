package pt.antares.app.feature.fooddata

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.util.rememberVoiceInput
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.quick_log_hint
import pt.antares.app.generated.resources.quick_log_photo
import pt.antares.app.generated.resources.quick_log_scan
import pt.antares.app.generated.resources.quick_log_voice
import pt.antares.app.generated.resources.quick_log_voice_prompt

@Composable
fun QuickLogBar(
    onSubmit: (String) -> Unit,
    onPhoto: () -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {

    var text by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    fun submit() {
        val q = text.trim()
        if (q.isEmpty()) return
        keyboard?.hide()
        onSubmit(q)
        text = ""
    }

    val voice = rememberVoiceInput { heard ->
        keyboard?.hide()
        onSubmit(heard)
        text = ""
    }
    val voicePrompt = stringResource(Res.string.quick_log_voice_prompt)

    OutlinedTextField(
        value = text,
        onValueChange = { text = it.take(80) },
        placeholder = { Text(stringResource(Res.string.quick_log_hint)) },
        // Decorativo: a lupa repete o rótulo do campo de pesquisa.
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            Row {
                if (voice.available) {
                    IconButton(onClick = { voice.start(voicePrompt) }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = stringResource(Res.string.quick_log_voice),
                        )
                    }
                }
                IconButton(onClick = onPhoto) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = stringResource(Res.string.quick_log_photo),
                    )
                }
                IconButton(onClick = onScan) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = stringResource(Res.string.quick_log_scan),
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(onSearch = { submit() }),
        modifier = modifier.fillMaxWidth(),
    )
}

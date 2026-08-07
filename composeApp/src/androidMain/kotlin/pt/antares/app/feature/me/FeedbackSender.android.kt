package pt.antares.app.feature.me

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import pt.antares.app.feature.about.AppChangelog

private const val FEEDBACK_EMAIL = "betuel801@gmail.com"

@Composable
actual fun rememberFeedbackSender(): () -> Unit {
    val context = LocalContext.current
    return {
        val subject = "Feedback Antares ${AppChangelog.CURRENT}"

        val body = buildString {
            append("\n\n---\n")
            append("Versão: ${AppChangelog.CURRENT}\n")
            append("Aparelho: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        }
        val uri = Uri.parse(
            "mailto:$FEEDBACK_EMAIL" +
                "?subject=${Uri.encode(subject)}" +
                "&body=${Uri.encode(body)}",
        )
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        runCatching { context.startActivity(intent) }
    }
}

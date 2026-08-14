package pt.antares.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import pt.antares.app.core.designsystem.LocalReduceMotion
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import pt.antares.app.feature.widget.AntaresWidgetProvider

class MainActivity : ComponentActivity() {

    // O idioma escolhido nas definições é aplicado aqui, antes de qualquer recurso ser
    // lido: mais tarde, os textos já teriam sido resolvidos no idioma do sistema.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(pt.antares.app.core.locale.LocalePrefs.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Escala de animação a zero é como o Android exprime "reduzir movimento". Lê-se uma
        // vez no arranque: mudá-la nas definições do sistema recria a atividade de qualquer
        // maneira.
        val reduceMotion = Settings.Global.getFloat(
            contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
        ) == 0f
        setContent {
            CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
                App()
            }
        }
    }

    // O alarme da meia-noite do [DayTicker] não corre com a app suspensa; voltar ao ecrã
    // no dia seguinte tem de acertar o dia à mesma.
    override fun onStart() {
        super.onStart()

        pt.antares.app.core.util.DayTicker.refresh()
    }

    // O widget atualiza-se ao sair e não a cada alteração: ele mostra o resumo do dia, e
    // redesenhá-lo a cada registo custaria mais do que vale.
    override fun onStop() {
        super.onStop()

        AntaresWidgetProvider.refresh(this)
    }
}

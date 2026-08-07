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

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(pt.antares.app.core.locale.LocalePrefs.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val reduceMotion = Settings.Global.getFloat(
            contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
        ) == 0f
        setContent {
            CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
                App()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        pt.antares.app.core.util.DayTicker.refresh()
    }

    override fun onStop() {
        super.onStop()

        AntaresWidgetProvider.refresh(this)
    }
}

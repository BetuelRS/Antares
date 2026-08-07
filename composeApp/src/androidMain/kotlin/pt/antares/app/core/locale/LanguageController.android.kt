package pt.antares.app.core.locale

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

object LocalePrefs {
    private const val FILE = "antares_locale"
    private const val KEY = "app_language"

    fun read(context: Context): AppLanguage {
        val tag = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
        return AppLanguage.fromTag(tag)
    }

    fun write(context: Context, language: AppLanguage) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY, language.tag).apply()
    }

    fun wrap(base: Context): Context {
        val language = read(base)
        val locale = when (language) {
            AppLanguage.SYSTEM -> systemLocaleOf(base)
            AppLanguage.PT -> Locale("pt")
            AppLanguage.EN -> Locale("en")
        }
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    @Suppress("DEPRECATION")
    private fun systemLocaleOf(context: Context): Locale {
        val config = context.resources.configuration
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.locales[0]
        } else {
            config.locale
        }
    }
}

fun Context.appLocalized(): Context = LocalePrefs.wrap(this)

@Composable
actual fun currentAppLanguage(): AppLanguage = LocalePrefs.read(LocalContext.current)

@Composable
actual fun rememberLanguageSetter(): (AppLanguage) -> Unit {
    val context = LocalContext.current
    return { language ->
        LocalePrefs.write(context, language)

        (context as? Activity)?.recreate()
    }
}

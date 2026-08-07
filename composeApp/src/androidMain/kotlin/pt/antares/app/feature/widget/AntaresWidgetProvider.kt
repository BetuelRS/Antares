package pt.antares.app.feature.widget

import android.app.PendingIntent
import pt.antares.app.core.locale.appLocalized
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.antares.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pt.antares.app.MainActivity
import pt.antares.app.core.database.daos.FoodLogDao
import pt.antares.app.core.database.daos.WaterLogDao
import pt.antares.app.core.database.daos.FastingSessionDao
import pt.antares.app.core.model.MealSlot
import pt.antares.app.feature.diary.DiaryRepository
import pt.antares.app.feature.profile.data.ProfileRepository
import pt.antares.app.core.util.currentHour
import pt.antares.app.core.util.todayEpochDay

class AntaresWidgetProvider : AppWidgetProvider(), KoinComponent {

    private val profileRepository: ProfileRepository by inject()
    private val foodLogDao: FoodLogDao by inject()
    private val waterDao: WaterLogDao by inject()
    private val fastingDao: FastingSessionDao by inject()
    private val diaryRepository: DiaryRepository by inject()

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateOne(context, manager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val acao = intent.action
        if (acao != ACTION_ADD_WATER && acao != ACTION_REPEAT_MEAL) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val hoje = todayEpochDay()
                when (acao) {
                    ACTION_ADD_WATER -> diaryRepository.addWater(hoje, WATER_STEP_ML)
                    ACTION_REPEAT_MEAL -> diaryRepository.repeatLastMeal(slotAgora(), hoje)
                }
            } finally {
                pending.finish()

                refresh(context)
            }
        }
    }

    private fun updateOne(context: Context, manager: AppWidgetManager, widgetId: Int) {

        val lctx = context.appLocalized()
        val views = RemoteViews(context.packageName, R.layout.widget_antares)

        val intent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(R.id.widget_root, intent)
        views.setOnClickPendingIntent(R.id.widget_add_water, broadcast(context, ACTION_ADD_WATER))

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = todayEpochDay()
                val target = runCatching { profileRepository.observeTargets(today).first() }.getOrNull()
                val totals = runCatching { foodLogDao.observeDayTotals(today).first() }.getOrNull()
                val water = runCatching { waterDao.byDay(today)?.ml }.getOrNull() ?: 0
                val fastingActive = runCatching { fastingDao.activeSession() }.getOrNull() != null

                val remaining = if (target != null && totals != null) target.kcal - totals.kcal else null
                views.setTextViewText(
                    R.id.widget_kcal,
                    remaining?.let { lctx.getString(R.string.widget_kcal_remaining, it) }
                        ?: lctx.getString(R.string.widget_kcal_none),
                )
                views.setTextViewText(R.id.widget_water, lctx.getString(R.string.widget_water, water))
                views.setTextViewText(
                    R.id.widget_fasting,
                    lctx.getString(if (fastingActive) R.string.widget_fasting_on else R.string.widget_fasting_off),
                )

                val repetivel = runCatching {
                    diaryRepository.lastMealBefore(slotAgora(), today)
                }.getOrNull()
                if (repetivel != null) {
                    views.setTextViewText(
                        R.id.widget_repeat,
                        lctx.getString(R.string.widget_repeat, repetivel.names.first()),
                    )
                    views.setOnClickPendingIntent(
                        R.id.widget_repeat,
                        broadcast(context, ACTION_REPEAT_MEAL),
                    )
                } else {
                    views.setTextViewText(R.id.widget_repeat, lctx.getString(R.string.widget_open))
                    views.setOnClickPendingIntent(R.id.widget_repeat, intent)
                }

                manager.updateAppWidget(widgetId, views)
            } finally {
                pending.finish()
            }
        }
    }

    private fun slotAgora(): MealSlot = MealSlot.atHour(currentHour())

    private fun broadcast(context: Context, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, AntaresWidgetProvider::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val ACTION_ADD_WATER = "pt.antares.app.widget.ADD_WATER"
        const val ACTION_REPEAT_MEAL = "pt.antares.app.widget.REPEAT_MEAL"

        const val WATER_STEP_ML = 250

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, AntaresWidgetProvider::class.java),
            )
            if (ids.isNotEmpty()) {
                val intent = Intent(context, AntaresWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}

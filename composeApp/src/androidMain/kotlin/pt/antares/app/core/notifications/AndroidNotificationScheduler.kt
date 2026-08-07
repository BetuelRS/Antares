package pt.antares.app.core.notifications

import android.app.NotificationChannel
import pt.antares.app.core.util.DayTicker
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.antares.app.R
import java.util.concurrent.TimeUnit

object AppNotificationChannels {
    const val MEAL = "meal_reminders"
    const val WEIGH_IN = "weigh_in_reminder"
    const val COACH = "coach_ready"

    fun ensureAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService<NotificationManager>() ?: return
        nm.createNotificationChannel(
            NotificationChannel(MEAL, context.getString(R.string.notif_channel_meal_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notif_channel_meal_desc)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(WEIGH_IN, context.getString(R.string.notif_channel_weighin_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notif_channel_weighin_desc)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(COACH, context.getString(R.string.notif_channel_coach_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notif_channel_coach_desc)
            },
        )
    }
}

object NotificationScheduler {

    private const val MEAL_WORK = "meal_reminders_periodic"
    private const val WEIGH_IN_WORK = "weighin_reminder_periodic"
    private const val WIDGET_WORK = "widget_midnight_refresh"
    private const val PROTEIN_WORK = "end_of_day_protein_periodic"

    fun scheduleAll(context: Context) {
        AppNotificationChannels.ensureAll(context)

        val meal = PeriodicWorkRequestBuilder<MealReminderWorker>(3, TimeUnit.HOURS)
            .setConstraints(Constraints.NONE)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(MEAL_WORK, ExistingPeriodicWorkPolicy.KEEP, meal)

        val weighIn = PeriodicWorkRequestBuilder<WeighInReminderWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.NONE)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WEIGH_IN_WORK, ExistingPeriodicWorkPolicy.KEEP, weighIn)

        val widgetDelay = DayTicker.msUntilNextMidnight() + 10 * 60 * 1000L
        val widget = PeriodicWorkRequestBuilder<WidgetMidnightWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(widgetDelay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.NONE)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WIDGET_WORK, ExistingPeriodicWorkPolicy.KEEP, widget)

        val toMidnight = DayTicker.msUntilNextMidnight()
        val before2030 = (3 * 60 + 30) * 60 * 1000L
        var proteinDelay = toMidnight - before2030
        if (proteinDelay <= 0) proteinDelay += 24 * 60 * 60 * 1000L
        val protein = PeriodicWorkRequestBuilder<EndOfDayProteinWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(proteinDelay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.NONE)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PROTEIN_WORK, ExistingPeriodicWorkPolicy.KEEP, protein)
    }
}

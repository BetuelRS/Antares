package pt.antares.app.core.notifications

import android.Manifest
import pt.antares.app.core.locale.appLocalized
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.antares.app.R
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.context.GlobalContext
import pt.antares.app.MainActivity
import pt.antares.app.core.calc.EndOfDayProtein
import pt.antares.app.core.database.daos.FoodLogDao
import pt.antares.app.core.database.daos.WeightLogDao
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.model.MealSlot
import pt.antares.app.feature.profile.data.ProfileRepository
import pt.antares.app.core.util.todayEpochDay

// Antes do Android 13 não havia permissão de notificações: era dada na instalação.
internal fun canPostNotifications(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

internal fun nowMinuteOfDay(): Int {
    val t = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return t.hour * 60 + t.minute
}

internal suspend fun inQuietHours(prefs: AppPreferences): Boolean {
    if (!prefs.quietHoursEnabled.first()) return false
    return NotificationRules.isQuiet(
        nowMinuteOfDay(),
        prefs.quietStartMin.first(),
        prefs.quietEndMin.first(),
    )
}

internal fun postNotification(context: Context, channel: String, id: Int, title: String, text: String) {
    // `FLAG_IMMUTABLE` é exigido desde o Android 12 e é o que impede outra app de alterar
    // o intent; `UPDATE_CURRENT` reaproveita o pendente em vez de acumular um por aviso.
    val tap = PendingIntent.getActivity(
        context, id, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val notif = NotificationCompat.Builder(context, channel)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(text)
        .setContentIntent(tap)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(id, notif)
}

class MealReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val koin = GlobalContext.get()
        val prefs = koin.get<AppPreferences>()
        val ctx = applicationContext.appLocalized()

        if (!prefs.mealReminders.first()) return Result.success()
        if (!canPostNotifications(ctx)) return Result.success()
        if (inQuietHours(prefs)) return Result.success()

        val slot = slotForHour(nowMinuteOfDay() / 60) ?: return Result.success()

        val foodDao = koin.get<FoodLogDao>()
        val logged = foodDao.loggedSlots(todayEpochDay()).toSet()
        if (!NotificationRules.shouldRemindMeal(slot, logged, enabled = true)) return Result.success()

        AppNotificationChannels.ensureAll(ctx)
        postNotification(
            ctx,
            AppNotificationChannels.MEAL,
            id = NOTIF_BASE + slot.ordinal,
            title = ctx.getString(R.string.notif_meal_title),
            text = ctx.getString(mealTextRes(slot)),
        )
        return Result.success()
    }

    private fun slotForHour(hour: Int): MealSlot? = when (hour) {
        in 8..10 -> MealSlot.BREAKFAST
        in 12..14 -> MealSlot.LUNCH
        in 19..21 -> MealSlot.DINNER
        else -> null
    }

    private fun mealTextRes(slot: MealSlot): Int = when (slot) {
        MealSlot.BREAKFAST -> R.string.notif_meal_breakfast
        MealSlot.LUNCH, MealSlot.SNACK -> R.string.notif_meal_lunch
        MealSlot.DINNER -> R.string.notif_meal_dinner
    }

    companion object {
        const val NOTIF_BASE = 5300
    }
}

class WeighInReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val koin = GlobalContext.get()
        val prefs = koin.get<AppPreferences>()
        val ctx = applicationContext.appLocalized()

        if (!prefs.weighInReminder.first()) return Result.success()
        if (!canPostNotifications(ctx)) return Result.success()
        if (inQuietHours(prefs)) return Result.success()

        val weightDao = koin.get<WeightLogDao>()
        val last = weightDao.latest()
        val today = todayEpochDay()

        val due = last == null || (today - last.epochDay) >= WEEK_DAYS
        if (!due) return Result.success()

        AppNotificationChannels.ensureAll(ctx)
        postNotification(
            ctx,
            AppNotificationChannels.WEIGH_IN,
            id = NOTIF_ID,
            title = ctx.getString(R.string.notif_weighin_title),
            text = ctx.getString(R.string.notif_weighin_text),
        )
        return Result.success()
    }

    companion object {
        const val NOTIF_ID = 5310
        const val WEEK_DAYS = 7
    }
}

class EndOfDayProteinWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val koin = GlobalContext.get()
        val prefs = koin.get<AppPreferences>()
        val ctx = applicationContext.appLocalized()

        if (!prefs.mealReminders.first()) return Result.success()
        if (!canPostNotifications(ctx)) return Result.success()
        if (inQuietHours(prefs)) return Result.success()

        val today = todayEpochDay()
        val foodDao = koin.get<FoodLogDao>()
        val hasLogs = foodDao.dayLogs(today).isNotEmpty()
        val consumed = foodDao.dayTotals(today).proteinG
        val target = koin.get<ProfileRepository>().targetsFor(today)?.proteinG ?: return Result.success()

        val gap = EndOfDayProtein.gapToNotify(consumed, target, hasLogs) ?: return Result.success()

        AppNotificationChannels.ensureAll(ctx)
        postNotification(
            ctx,
            AppNotificationChannels.MEAL,
            id = NOTIF_ID,
            title = ctx.getString(R.string.notif_protein_title),
            text = ctx.getString(R.string.notif_protein_text, gap),
        )
        return Result.success()
    }

    companion object {
        const val NOTIF_ID = 5320
    }
}

class WidgetMidnightWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        pt.antares.app.feature.widget.AntaresWidgetProvider.refresh(applicationContext)
        return Result.success()
    }
}

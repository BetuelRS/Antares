package pt.antares.app.core.notifications

import android.content.Context
import pt.antares.app.core.locale.appLocalized
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.CoroutineWorker
import com.antares.app.R
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import pt.antares.app.core.datastore.AppPreferences

class AndroidCoachNotifier(private val context: Context) : CoachNotifier {
    override fun notifyReportReady() {
        val request = OneTimeWorkRequestBuilder<CoachReadyWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}

class CoachReadyWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val koin = GlobalContext.get()
        val prefs = koin.get<AppPreferences>()
        val ctx = applicationContext.appLocalized()

        if (!prefs.coachReadyNotif.first()) return Result.success()
        if (!canPostNotifications(ctx)) return Result.success()
        if (inQuietHours(prefs)) return Result.success()

        AppNotificationChannels.ensureAll(ctx)
        postNotification(
            ctx,
            AppNotificationChannels.COACH,
            id = NOTIF_ID,
            title = ctx.getString(R.string.notif_coach_title),
            text = ctx.getString(R.string.notif_coach_text),
        )
        return Result.success()
    }

    companion object {
        const val NOTIF_ID = 5320
    }
}

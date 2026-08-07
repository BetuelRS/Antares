package pt.antares.app.feature.fasting

import android.Manifest
import pt.antares.app.core.locale.appLocalized
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import pt.antares.app.MainActivity
import pt.antares.app.core.datastore.AppPreferences
import com.antares.app.R
import java.util.concurrent.TimeUnit

internal object FastingChannel {
    const val ID = "fasting_goal"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService<NotificationManager>() ?: return
        nm.createNotificationChannel(
            NotificationChannel(ID, context.getString(R.string.notif_channel_fasting_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notif_channel_fasting_desc)
            },
        )
    }
}

class AndroidFastingNotifier(private val context: Context) : FastingNotifier {

    private fun workName(sessionId: String) = "fasting_goal_$sessionId"

    override fun scheduleGoal(sessionId: String, targetEndAt: Long) {
        val delay = (targetEndAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<FastingGoalWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(FastingGoalWorker.KEY_SESSION to sessionId))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(sessionId), ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(sessionId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(sessionId))
    }
}

class FastingGoalWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = GlobalContext.get().get<AppPreferences>()
        if (!prefs.fastingNotifications.first()) return Result.success()

        val ctx = applicationContext.appLocalized()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        FastingChannel.ensure(ctx)
        val tapIntent = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(ctx, FastingChannel.ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(ctx.getString(R.string.notif_fasting_goal_title))
            .setContentText(ctx.getString(R.string.notif_fasting_goal_text))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        val sessionId = inputData.getString(KEY_SESSION) ?: "fasting"
        NotificationManagerCompat.from(ctx).notify(sessionId.hashCode(), notif)
        return Result.success()
    }

    companion object {
        const val KEY_SESSION = "session_id"
    }
}

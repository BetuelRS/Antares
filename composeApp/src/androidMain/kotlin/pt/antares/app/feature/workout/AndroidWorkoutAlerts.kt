package pt.antares.app.feature.workout

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.antares.app.R
import pt.antares.app.MainActivity

/**
 * Dois canais com importâncias opostas de propósito: o fim do descanso tem de interromper
 * — é o ponto —, e o aviso de treino em curso é permanente e não pode fazer barulho de
 * cada vez que se atualiza.
 */
internal object WorkoutChannels {
    const val REST = "workout_rest"
    const val SESSION = "workout_session"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService<NotificationManager>() ?: return

        nm.createNotificationChannel(
            NotificationChannel(REST, context.getString(R.string.notif_channel_rest_name), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.notif_channel_rest_desc)
                enableVibration(true)
            },
        )

        nm.createNotificationChannel(
            NotificationChannel(SESSION, context.getString(R.string.notif_channel_session_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.notif_channel_session_desc)
            },
        )
    }
}

internal fun Context.vibrateShort() {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService<VibratorManager>()?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") getSystemService<Vibrator>()
    } ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION") vibrator.vibrate(400)
    }
}

class AndroidWorkoutAlerts(private val context: Context) : WorkoutAlerts {

    private val nm = context.getSystemService<NotificationManager>()
    private val alarmManager = context.getSystemService<AlarmManager>()

    init {
        WorkoutChannels.ensure(context)
    }

    private fun restPendingIntent(): PendingIntent {
        val intent = Intent(context, RestEndReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REST_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun scheduleRestEnd(seconds: Int) {
        val am = alarmManager ?: return
        val triggerAt = System.currentTimeMillis() + seconds * 1000L
        val pi = restPendingIntent()
        try {
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (exact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {

            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    override fun cancelRestEnd() {
        alarmManager?.cancel(restPendingIntent())
        nm?.cancel(REST_NOTIF_ID)
    }

    override fun setSessionOngoing(active: Boolean) {
        if (!active) {
            nm?.cancel(SESSION_NOTIF_ID)
            return
        }
        WorkoutChannels.ensure(context)
        val open = PendingIntent.getActivity(
            context, SESSION_REQUEST,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, WorkoutChannels.SESSION)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(context.getString(R.string.notif_session_title))
            .setContentText(context.getString(R.string.notif_session_text))
            .setOngoing(true)
            .setContentIntent(open)
            .build()
        nm?.notify(SESSION_NOTIF_ID, notif)
    }

    companion object {
        const val REST_NOTIF_ID = 2001
        const val SESSION_NOTIF_ID = 2002
        const val REST_REQUEST = 3001
        const val SESSION_REQUEST = 3002
    }
}

class RestEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WorkoutChannels.ensure(context)
        context.vibrateShort()
        val nm = context.getSystemService<NotificationManager>() ?: return
        val notif = NotificationCompat.Builder(context, WorkoutChannels.REST)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notif_rest_done_title))
            .setContentText(context.getString(R.string.notif_rest_done_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 400))
            .build()
        nm.notify(AndroidWorkoutAlerts.REST_NOTIF_ID, notif)
    }
}

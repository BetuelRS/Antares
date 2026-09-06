package pt.antares.app.feature.running

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.antares.app.R
import pt.antares.app.MainActivity

/**
 * Serviço em primeiro plano que mantém o GPS a correr com o ecrã apagado. Não é opção: o
 * Android suspende uma app em segundo plano em segundos, e sem isto a corrida perdia-se
 * assim que o telemóvel entrasse no bolso.
 *
 * A notificação permanente é a contrapartida exigida pelo sistema — e é justa: é o que diz
 * à pessoa que a localização está a ser lida.
 */
class RunTrackerService : Service() {

    private var source: RunLocationSource? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        ensureChannel(this)
        startForegroundCompat()
        requestUpdates()
        // `START_STICKY` para o sistema recriar o serviço se o matar por falta de memória:
        // uma corrida de uma hora não pode acabar porque outra app precisou de espaço.
        return START_STICKY
    }

    private fun requestUpdates() {
        // O serviço pode receber vários arranques — o sistema reenvia o intent ao recriar —
        // e sem isto abriam-se várias subscrições de GPS sobre a mesma corrida.
        if (source != null) return
        try {
            source = RunLocationSource.create(this).also { s ->
                s.start { sample ->
                    RunTrackingState.onSample(sample)
                    updateNotification()
                }
            }
        } catch (_: SecurityException) {

            // Permissão de localização revogada com o serviço já a correr. Pára-se em
            // silêncio: insistir seria mostrar uma notificação permanente sem GPS por trás.
            source = null
            stopSelf()
        }
    }

    override fun onDestroy() {
        source?.stop()
        source = null
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val notif = buildNotification()
        // A partir do Android 14 é obrigatório declarar o tipo de serviço em primeiro plano;
        // sem ele o sistema recusa o arranque e mata a app.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun updateNotification() {
        getSystemService<NotificationManager>()?.notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val m = RunTrackingState.live.value.metrics
        val km = m.distanceM / 1000.0
        val sec = m.movingMs / 1000
        // Em pausa, a notificação **diz que está em pausa**. É a cara da app com o ecrã
        // apagado: sem isto, quem pausou e guardou o telemóvel via dois números parados sem
        // nada que explicasse porquê — e o mais provável era pensar que a gravação morreu.
        val base = "${formatKm(km)} · ${formatClock(sec)}"
        val text = if (m.pausaManual) "$base · ${getString(R.string.notif_run_paused)}" else base
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.notif_run_title))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(tap)
            .build()
    }

    companion object {
        const val CHANNEL = "run_tracking"
        const val NOTIF_ID = 4201
        const val ACTION_STOP = "pt.antares.app.RUN_STOP"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService<NotificationManager>() ?: return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.notif_channel_run_name), NotificationManager.IMPORTANCE_LOW).apply {
                    description = context.getString(R.string.notif_channel_run_desc)
                },
            )
        }
    }
}

private fun formatKm(km: Double): String {
    val whole = km.toInt()
    val dec = ((km - whole) * 100).toInt()
    val d = if (dec < 10) "0$dec" else "$dec"
    return "$whole,$d km"
}

private fun formatClock(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    fun p(n: Long) = if (n < 10) "0$n" else "$n"
    return if (h > 0) "${p(h)}:${p(m)}:${p(s)}" else "${p(m)}:${p(s)}"
}

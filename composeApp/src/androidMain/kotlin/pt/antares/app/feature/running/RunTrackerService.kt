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
import pt.antares.app.core.designsystem.usaVirgulaDecimal
import pt.antares.app.core.locale.appLocalized
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.feature.running.domain.AvisoDaCorrida
import pt.antares.app.feature.running.domain.Split
import pt.antares.app.feature.running.ui.RunFormat

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

    /**
     * A voz vive aqui, e não no ecrã, pela mesma razão que o GPS: quem corre tem o ecrã
     * apagado, e um aviso dito a partir de uma composição calava-se dentro do bolso.
     *
     * **Nasce com o serviço e não à primeira frase**, e isso é o que faz o quilómetro 1 ser
     * dito: o motor de voz demora perto de um segundo a ligar-se, e um locutor criado no
     * instante do aviso responde que ainda não está pronto — e cala precisamente o primeiro.
     */
    private var locutor: Locutor? = null

    /**
     * O contexto no idioma da app, e o que dele se deriva.
     *
     * Guardados, e não pedidos a cada amostra: o `appLocalized()` constrói um contexto novo
     * de cada vez e mexe no `Locale` global do processo. Chamá-lo por cada posição do GPS
     * era uma vez por segundo durante a corrida inteira.
     */
    private val ctxLocalizado by lazy { applicationContext.appLocalized() }
    private val idioma by lazy { ctxLocalizado.resources.configuration.locales[0] }

    // Quantos parciais já foram anunciados. É a memória que transforma «a lista cresceu» em
    // «há coisa nova para dizer» — o motor não emite eventos, guarda uma lista.
    private var anunciados = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locutor = Locutor(this)

        // O serviço é `START_STICKY`: o sistema pode matá-lo por falta de memória e
        // recriá-lo com a corrida a meio. Sem esta linha, o serviço novo nascia a achar que
        // nada tinha sido dito e repetia o último quilómetro à primeira posição que
        // recebesse.
        anunciados = RunTrackingState.live.value.parciais.size
    }

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
                    anunciarSeHouverNovidade()
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
        locutor?.fechar()
        locutor = null
        super.onDestroy()
    }

    /**
     * Diz o parcial que fechou, se fechou algum.
     *
     * Chamado a cada amostra do GPS — que é a única batida que este serviço tem. O motor não
     * avisa quando fecha um quilómetro: acrescenta-o a uma lista, e o que faz disso um evento
     * é comparar o tamanho dela com o que já se disse.
     */
    private fun anunciarSeHouverNovidade() {
        val live = RunTrackingState.live.value
        val parciais = live.parciais
        val novo = AvisoDaCorrida.porAnunciar(parciais, anunciados) ?: return
        // Marca-se **antes** de falar, e marca-se a lista inteira: os que ficaram para trás
        // não se dizem, e um erro do motor de voz não pode fazer a frase repetir-se a cada
        // amostra seguinte.
        anunciados = parciais.size

        locutor?.dizer(fraseDoAviso(ctxLocalizado, novo, live.metrics.movingMs), idioma)
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
        val live = RunTrackingState.live.value
        val m = live.metrics

        // O idioma da app, e não o do telemóvel. Os cinco trabalhadores de notificação, o
        // aviso do treinador, o do jejum e o widget fazem todos isto; **este era o único
        // que não fazia**, e quem tivesse a app em português num telemóvel inglês via esta
        // notificação — e só esta — em inglês.
        val ctx = ctxLocalizado

        // As mesmas contas do ecrã, e não umas próprias. As de antes truncavam em vez de
        // arredondar, escreviam a vírgula à mão e mediam sempre em quilómetros: na mesma
        // corrida, o ecrã dizia `0.06 km` e a notificação `0,05 km`.
        val virgula = usaVirgulaDecimal(idioma.language)
        val distancia = RunFormat.distance(m.distanceM, live.unidades, virgula)
        val unidade = ctx.getString(unidadeDeDistancia(live.unidades))

        // Em pausa, a notificação **diz que está em pausa**. É a cara da app com o ecrã
        // apagado: sem isto, quem pausou e guardou o telemóvel via dois números parados sem
        // nada que explicasse porquê — e o mais provável era pensar que a gravação morreu.
        val base = "$distancia $unidade · ${RunFormat.clock(m.movingMs)}"
        val text = if (m.pausaManual) "$base · ${ctx.getString(R.string.notif_run_paused)}" else base
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(ctx.getString(R.string.notif_run_title))
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

/**
 * A frase que a voz diz, montada dos recursos e dos números que o [AvisoDaCorrida] parte.
 *
 * Fora da classe do serviço de propósito: é a única parte desta versão que a pessoa **ouve**,
 * e um serviço não se instancia num teste. Assim prova-se a frase inteira, nas duas línguas,
 * sem emulador.
 *
 * As três partes são frases inteiras e não pedaços: um aviso sem ritmo — porque ainda não há
 * um metro andado neste parcial — continua a ser uma frase, em vez de ficar com um buraco no
 * meio.
 */
internal fun fraseDoAviso(ctx: Context, s: Split, movingMs: Long): String {
    val cabeca = ctx.getString(
        if (s.manual) R.string.run_voice_lap else R.string.run_voice_km,
        s.index,
    )
    val ritmo = AvisoDaCorrida.ritmo(s.paceSecPerKm)
        ?.let { ctx.getString(R.string.run_voice_pace, it.minutos, it.segundos) }
    val t = AvisoDaCorrida.tempo(movingMs)
    val tempo = if (t.horas > 0) {
        ctx.resources.getQuantityString(R.plurals.run_voice_time_h, t.horas, t.horas, t.minutos)
    } else {
        ctx.resources.getQuantityString(R.plurals.run_voice_time_min, t.minutos, t.minutos)
    }
    return listOfNotNull(cabeca, ritmo, tempo).joinToString(" ")
}

/**
 * O rótulo da unidade de distância, do lado do Android.
 *
 * O ecrã tem o `distanceUnitLabel` dos recursos do Compose, e esses não se leem de um
 * serviço. São as mesmas duas palavras, e é por isso que ficam aqui e não numa terceira
 * ideia de como se escreve um quilómetro.
 */
private fun unidadeDeDistancia(unidades: UnitSystem): Int =
    if (unidades == UnitSystem.IMPERIAL) R.string.notif_unit_mi else R.string.notif_unit_km

package pt.antares.app.feature.running

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.getSystemService
import java.util.Locale

/**
 * A voz da corrida.
 *
 * Vive no `androidMain` e não tem par no código comum, porque o que ela serve também não
 * tem: quem a ouve tem o ecrã apagado e o telemóvel no bolso, e isso é o serviço em
 * primeiro plano. Um `expect`/`actual` aqui era uma abstração sobre um alvo só.
 *
 * **Pede o foco de áudio a baixar o som em vez de o cortar.** Quem corre corre com música, e
 * uma frase de quatro segundos que cale a música e a devolva a seguir é pior do que o
 * silêncio — o `AudioManager` sabe baixá-la e repô-la sozinho, e é isso que se lhe pede.
 */
internal class Locutor(context: Context) {

    private val audio = context.getSystemService<AudioManager>()

    private val atributos = AudioAttributes.Builder()
        // «Orientação» e não «media»: é a categoria dos navegadores, e é a que os leitores
        // de música sabem baixar. Marcada como media, a frase disputava o volume em vez de
        // passar por cima dele.
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val pedidoDeFoco = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(atributos)
        .build()

    private var pronto = false

    // O arranque do motor de voz é assíncrono e demora perto de um segundo. Começa aqui, ao
    // criar o serviço, e não no primeiro quilómetro: quem chega ao km 1 já o tem pronto.
    private val tts = TextToSpeech(context.applicationContext) { estado ->
        pronto = estado == TextToSpeech.SUCCESS
    }.apply {
        setAudioAttributes(atributos)

        // Registado **uma vez**, e não a cada frase: pô-lo dentro do `dizer` substituía o
        // anterior a meio de uma frase que ainda estava a ser lida, e o `onDone` dessa
        // deixava de chegar a alguém — a música ficava baixa até ao fim da corrida.
        setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = largarOFoco()

            @Deprecated("A assinatura sem erro continua a ser a que o sistema chama.")
            override fun onError(utteranceId: String?) = largarOFoco()
        })
    }

    /**
     * Diz uma frase, se houver motor de voz.
     *
     * `QUEUE_ADD` e não `QUEUE_FLUSH`: duas frases seguidas são raras — só se anuncia o
     * último parcial — e cortar a primeira a meio para dizer a segunda é a única maneira de
     * as duas ficarem por perceber.
     */
    fun dizer(texto: String, idioma: Locale) {
        if (!pronto) return
        // Um idioma que o motor não tenha lê-se com a pronúncia errada ou não se lê de todo.
        // Nesse caso cala-se: uma frase em português dita por uma voz inglesa a correr é
        // pior do que não dizer nada.
        if (tts.setLanguage(idioma) == TextToSpeech.LANG_NOT_SUPPORTED) return

        // O foco pede-se antes de falar e larga-se no `onDone`, e não a seguir ao `speak`:
        // a fala é assíncrona, e largá-lo à partida devolvia a música ao volume por cima da
        // frase que estava a começar.
        audio?.requestAudioFocus(pedidoDeFoco)
        tts.speak(texto, TextToSpeech.QUEUE_ADD, null, ID_DO_AVISO)
    }

    fun fechar() {
        largarOFoco()
        tts.stop()
        tts.shutdown()
    }

    private fun largarOFoco() {
        audio?.abandonAudioFocusRequest(pedidoDeFoco)
    }

    private companion object {
        const val ID_DO_AVISO = "antares-aviso-da-corrida"
    }
}

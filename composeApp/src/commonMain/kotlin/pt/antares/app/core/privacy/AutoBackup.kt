package pt.antares.app.core.privacy

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pt.antares.app.core.database.daos.ProgressPhotoDao
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.util.LocalPhotoStore
import kotlin.time.Duration.Companion.days

/**
 * O que o ecrã precisa de saber sobre a última cópia. Nulo em [ultimaEm] não é «hoje» nem
 * «erro»: é «nunca aconteceu», que é o estado de quem acabou de instalar a app.
 */
data class EstadoDaCopia(
    val ultimaEm: Instant?,
    val diasDesde: Int?,
    val nome: String?,
    val pasta: String,
    val podeEscrever: Boolean,
    val quantas: Int,
) {
    /** Vermelho no cartão. Nunca ter havido cópia conta como atraso, e é o pior caso. */
    val atrasada: Boolean
        get() = diasDesde == null || diasDesde >= AutoBackup.DIAS_ATE_AVISAR
}

/**
 * A cópia de segurança que ninguém tem de se lembrar de fazer.
 *
 * Substituiu a cópia automática da Google, desligada na 2.1.0. É por isso que o ficheiro vai
 * para `Documentos/Antares` e não para dentro da app: o que a cópia da Google dava e uma
 * exportação manual não dá é sobreviver a uma reinstalação.
 *
 * Corre no arranque e não num temporizador. Um trabalho agendado que o fabricante mata é
 * pior do que não haver trabalho nenhum, porque o cartão diria que está tudo bem.
 */
class AutoBackup(
    private val prefs: AppPreferences,
    private val exporter: DataExporter,
    private val photoDao: ProgressPhotoDao,
    private val photos: LocalPhotoStore,
    private val store: BackupStore,
) {

    suspend fun estado(): EstadoDaCopia {
        val quando = prefs.lastBackupAtOnce().takeIf { it > 0 }?.let(Instant::fromEpochMilliseconds)
        val agora = Clock.System.now()
        return EstadoDaCopia(
            ultimaEm = quando,
            diasDesde = quando?.let { ((agora - it).inWholeDays).toInt().coerceAtLeast(0) },
            nome = prefs.lastBackupNameOnce().takeIf { it.isNotBlank() },
            pasta = store.describe(),
            podeEscrever = store.canWrite(),
            quantas = store.list().size,
        )
    }

    /**
     * Corre se já passou tempo que chegue — ou se nunca correu, que é o caso de quem acaba
     * de atualizar da 2.0.4. Essa primeira cópia é a condição para desligar a da Google não
     * deixar ninguém a descoberto, e por isso não espera pelos três dias.
     *
     * Nunca antes do arranque estar feito. A 2.1.0 saiu sem esta guarda e escrevia a
     * primeira cópia no primeiro segundo de uma instalação limpa: um ficheiro de 526 bytes,
     * vinte e seis tabelas e zero linhas, com o cartão a dizer «última cópia: hoje». Uma
     * cópia que não protege nada é pior do que nenhuma, porque cala o aviso.
     */
    suspend fun correrSeNecessario(): Boolean {
        if (!store.canWrite()) return false
        val deve = deveCorrer(
            arranqueFeito = prefs.onboardingDoneOnce(),
            ultimaMs = prefs.lastBackupAtOnce(),
            agoraMs = Clock.System.now().toEpochMilliseconds(),
        )
        if (!deve) return false
        return correrAgora()
    }

    suspend fun correrAgora(): Boolean {
        if (!store.canWrite()) return false
        val agora = Clock.System.now()
        val gravado = store.write(nomeDe(agora), conteudo()) ?: return false
        prefs.setLastBackup(agora.toEpochMilliseconds(), gravado)
        rodar()
        return true
    }

    /**
     * As cópias a mais são apagadas depois de a nova estar escrita, e nunca antes: se o
     * disco estiver cheio, é melhor falhar a nova e ficar com as cinco antigas do que abrir
     * espaço para uma cópia que não chega a existir.
     */
    private suspend fun rodar() {
        aApagar(store.list(), MAX_COPIAS).forEach { store.delete(it) }
    }

    private suspend fun conteudo(): Map<String, ByteArray> {
        val entradas = LinkedHashMap<String, ByteArray>()
        entradas[BackupFiles.DATA] = exporter.exportJson().encodeToByteArray()
        exporter.exportCsvFiles().forEach { (nome, csv) ->
            entradas[nome] = csv.encodeToByteArray()
        }
        photoDao.all().forEach { foto ->
            photos.readBytes(foto.localPath)?.let { bytes ->
                entradas[BackupFiles.PHOTO_DIR + foto.id + ".jpg"] = bytes
            }
        }
        return entradas
    }

    companion object {

        // O nome é o que dá a ordem: em texto, esta data ordena-se sozinha da mais antiga
        // para a mais recente, e a rotação não precisa de ler a data de cada ficheiro.
        const val PREFIXO = "antares-copia-"

        const val MAX_COPIAS = 5
        const val DIAS_ENTRE_COPIAS = 3
        const val DIAS_ATE_AVISAR = 7

        // A hora é a local e não a UTC: o nome é para uma pessoa reconhecer a cópia numa
        // pasta, e «ontem à noite» tem de se ler como ontem. O fuso entra por parâmetro só
        // para os testes poderem fixar um — senão passavam ou falhavam conforme a máquina.
        fun nomeDe(quando: Instant, fuso: TimeZone = TimeZone.currentSystemDefault()): String {
            val t = quando.toLocalDateTime(fuso)
            val d = "${t.year}-${dois(t.monthNumber)}-${dois(t.dayOfMonth)}"
            // Com os segundos e não só até ao minuto: duas cópias seguidas no mesmo minuto
            // dão o mesmo nome, e o MediaStore resolve-o sozinho gravando «… (1).zip» — um
            // nome que já não se ordena pela data e que a app não pediu.
            return "$PREFIXO$d-${dois(t.hour)}${dois(t.minute)}${dois(t.second)}.zip"
        }

        // Dois algarismos sempre, porque é o zero à frente que faz a ordem alfabética dos
        // nomes coincidir com a cronológica — e é nisso que a rotação assenta.
        private const val UM_ALGARISMO = 10

        private fun dois(n: Int): String = if (n < UM_ALGARISMO) "0$n" else "$n"

        /**
         * As cópias a apagar para ficarem [maximo]. Ordena por nome de propósito: é o
         * formato de [nomeDe] que faz a ordem alfabética coincidir com a cronológica, e
         * confiar na ordem com que a pasta é listada seria confiar no sistema de ficheiros.
         */
        fun aApagar(existentes: List<String>, maximo: Int = MAX_COPIAS): List<String> =
            existentes.sorted().dropLast(maximo)

        /**
         * Se a cópia automática é devida. Fora da classe e sem tocar em nada para poder ser
         * provada: é a decisão que a 2.1.0 errou, e um teste que precise de um MediaStore
         * para a verificar não se escreve.
         *
         * Nunca antes do arranque estar feito — não há o que copiar, e uma cópia vazia cala
         * o aviso sem proteger ninguém.
         */
        fun deveCorrer(
            arranqueFeito: Boolean,
            ultimaMs: Long,
            agoraMs: Long,
            dias: Int = DIAS_ENTRE_COPIAS,
        ): Boolean {
            if (!arranqueFeito) return false
            if (ultimaMs <= 0L) return true
            return agoraMs - ultimaMs >= dias.days.inWholeMilliseconds
        }
    }
}

package pt.antares.app.feature.profile.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.antares.app.core.database.daos.BodyMeasurementDao
import pt.antares.app.core.database.daos.UserProfileDao
import pt.antares.app.core.database.entities.BodyMeasurementEntity
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.todayEpochDay

/**
 * O histórico de medições do corpo, e a única porta por onde a massa gorda entra na app.
 *
 * **A tabela é a verdade; o `user_profile.bodyFatPct` é uma cópia.** Existe porque as
 * contas do dia — a Katch-McArdle, a massa magra — precisam do valor sem ir ao histórico a
 * cada ecrã. Mas quem manda é a medição mais recente, e por isso toda a escrita aqui
 * reescreve essa cópia: era assim que os dois se conseguiam contradizer.
 */
class BodyMeasurementRepository(
    private val dao: BodyMeasurementDao,
    private val profileDao: UserProfileDao,
    private val io: CoroutineDispatcher,
) {

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    fun observeAll(): Flow<List<BodyMeasurementEntity>> = dao.observeAll()

    fun observeLatest(): Flow<BodyMeasurementEntity?> = dao.observeLatest()

    suspend fun latest(): BodyMeasurementEntity? = withContext(io) { dao.latest() }

    /**
     * Grava as medidas do dia.
     *
     * Cada campo a nulo quer dizer «não medi isto agora», e o que já lá estava fica —
     * registar só a cintura hoje não pode apagar a massa gorda medida no mesmo dia.
     *
     * [apagarGordura] é a exceção, e existe porque nulo não chega para dizer duas coisas
     * diferentes: quem escolhe «não sei» está a mandar apagar, e não a omitir. Sem isto, a
     * escolha limpava o perfil e deixava o histórico do dia intacto — que é como a app
     * passava a mostrar dois valores diferentes para a mesma coisa.
     */
    suspend fun record(
        epochDay: Long = todayEpochDay(),
        bodyFatPct: Double? = null,
        bodyFatSource: BodyFatSource? = null,
        waistCm: Double? = null,
        neckCm: Double? = null,
        hipCm: Double? = null,
        armCm: Double? = null,
        thighCm: Double? = null,
        chestCm: Double? = null,
        apagarGordura: Boolean = false,
    ) = withContext(io) {

        // Vê as lápides, para reaproveitar a linha do dia — o índice único conta-as.
        val row = dao.byDayForWrite(epochDay)

        // Mas só uma linha viva contribui com valores: reabrir um dia apagado começa
        // do zero em vez de ressuscitar medidas que a pessoa desfez.
        val existing = row?.takeIf { !it.deleted }
        val merged = BodyMeasurementEntity(
            id = row?.id ?: Ids.newUuid(),
            epochDay = epochDay,
            bodyFatPct = if (apagarGordura) null else bodyFatPct ?: existing?.bodyFatPct,
            bodyFatSource = if (apagarGordura) null else bodyFatSource ?: existing?.bodyFatSource,
            waistCm = waistCm ?: existing?.waistCm,
            neckCm = neckCm ?: existing?.neckCm,
            hipCm = hipCm ?: existing?.hipCm,
            armCm = armCm ?: existing?.armCm,
            thighCm = thighCm ?: existing?.thighCm,
            chestCm = chestCm ?: existing?.chestCm,
            updatedAt = now(),
        )

        when {
            !merged.isEmpty -> dao.upsert(merged)

            // Tirar a última medida de um dia apaga a linha, em vez de a deixar como
            // estava. Sem isto o histórico ficava com o valor que a pessoa retirou, e como
            // é ele a fonte, o valor voltava ao perfil no cálculo seguinte.
            existing != null -> dao.softDelete(existing.id, now())

            // Nada escrito e nada que existisse: não se cria um dia vazio.
            else -> Unit
        }
        sincronizarPerfil()
    }

    suspend fun delete(id: String) = withContext(io) {
        dao.softDelete(id, now())
        sincronizarPerfil()
    }

    suspend fun restore(id: String) = withContext(io) {
        dao.restore(id, now())
        // A massa gorda do perfil segue a medição viva mais recente: sem isto, desfazer
        // devolvia a linha e deixava o perfil a olhar para a anterior.
        sincronizarPerfil()
    }

    /**
     * Repõe no perfil a massa gorda da medição viva mais recente, ou tira-a se já não
     * houver nenhuma. É a única escrita da app nesses dois campos.
     */
    private suspend fun sincronizarPerfil() {
        val perfil = profileDao.get() ?: return
        val medicao = dao.latestWithBodyFat()
        if (perfil.bodyFatPct == medicao?.bodyFatPct && perfil.bodyFatSource == medicao?.bodyFatSource) {
            return
        }
        profileDao.upsert(
            perfil.copy(
                bodyFatPct = medicao?.bodyFatPct,
                bodyFatSource = medicao?.bodyFatSource,
                updatedAt = now(),
            ),
        )
    }
}

package pt.antares.app.feature.templates

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.antares.app.core.database.daos.FoodLogDao
import pt.antares.app.core.database.daos.MealTemplateDao
import pt.antares.app.core.database.daos.MealTemplateItemDao
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.database.entities.MealTemplateEntity
import pt.antares.app.core.database.entities.MealTemplateItemEntity
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.currentMinuteOfDay
import pt.antares.app.core.util.todayEpochDay

/**
 * Refeições guardadas para repetir. Um modelo é uma cópia congelada de um dia: guardar
 * copia os registos para o modelo, aplicar copia-os de volta para outro dia. Em nenhum dos
 * sentidos se vai buscar nada ao catálogo — é o que faz o modelo dar sempre o mesmo
 * resultado, mesmo que o alimento mude ou seja apagado.
 */
/**
 * Um item a caminho de um modelo, sem vir de lado nenhum da base.
 *
 * Existe para o [MealTemplateRepository.saveItemsAsTemplate] poder receber o que a folha da
 * AI tem em mão sem que os modelos passem a conhecer o pacote da AI — o que os obrigaria a
 * mudar de cada vez que o contrato com o servidor mudasse.
 */
data class ItemDeModelo(
    val nome: String,
    val gramas: Double,
    val kcal: Int,
    val proteina: Double,
    val hidratos: Double,
    val gordura: Double,
    val microsPer100Json: String? = null,
    val liquido: Boolean = false,
    val foodId: String? = null,
)

class MealTemplateRepository(
    private val foodLogDao: FoodLogDao,
    private val templateDao: MealTemplateDao,
    private val itemDao: MealTemplateItemDao,
    private val io: CoroutineDispatcher,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val newId: () -> String = { Ids.newUuid() },
) {

    fun observeTemplates(): Flow<List<MealTemplateEntity>> = templateDao.observeAll()

    suspend fun items(templateId: String): List<MealTemplateItemEntity> =
        withContext(io) { itemDao.forTemplate(templateId) }

    /**
     * Guarda uma lista de itens como modelo, **sem passar pelo diário**.
     *
     * O [saveMealAsTemplate] lê o slot inteiro do dia, e é o que se quer quando se guarda
     * uma refeição já registada. Não serve para guardar o que se acabou de rever na folha
     * da AI: quem já tinha registado o pão às oito ficava com ele dentro de um modelo
     * chamado «Almoço», e o nome deixava de descrever o que lá está.
     */
    suspend fun saveItemsAsTemplate(
        name: String,
        slot: MealSlot,
        itens: List<ItemDeModelo>,
    ): String? = withContext(io) {
        if (itens.isEmpty()) return@withContext null

        val templateId = newId()
        val ts = now()
        templateDao.upsert(
            MealTemplateEntity(id = templateId, name = name.trim(), slot = slot, updatedAt = ts),
        )
        itemDao.upsertAll(
            itens.map { item ->
                MealTemplateItemEntity(
                    id = newId(),
                    templateId = templateId,
                    foodId = item.foodId,
                    nameSnapshot = item.nome,
                    quantityGrams = item.gramas,
                    kcalSnapshot = item.kcal,
                    proteinSnapshot = item.proteina,
                    carbsSnapshot = item.hidratos,
                    fatSnapshot = item.gordura,
                    microsPer100Json = item.microsPer100Json,
                    isLiquid = item.liquido,
                    updatedAt = ts,
                )
            },
        )
        templateId
    }

    suspend fun saveMealAsTemplate(name: String, slot: MealSlot, epochDay: Long): String? =
        withContext(io) {
            // Null para uma refeição vazia: não há nada que valha a pena guardar, e um
            // modelo sem itens só ocuparia a lista.
            val logs = foodLogDao.mealLogs(epochDay, slot)
            if (logs.isEmpty()) return@withContext null

            val templateId = newId()
            val ts = now()
            templateDao.upsert(
                MealTemplateEntity(
                    id = templateId,
                    name = name.trim(),
                    slot = slot,
                    updatedAt = ts,
                ),
            )
            itemDao.upsertAll(
                logs.map { log ->
                    MealTemplateItemEntity(
                        id = newId(),
                        templateId = templateId,
                        foodId = log.foodId,
                        nameSnapshot = log.nameSnapshot,
                        quantityGrams = log.quantityGrams,
                        kcalSnapshot = log.kcalSnapshot,
                        proteinSnapshot = log.proteinSnapshot,
                        carbsSnapshot = log.carbsSnapshot,
                        fatSnapshot = log.fatSnapshot,
                        microsPer100Json = log.microsPer100Json,
                        isLiquid = log.isLiquid,
                        updatedAt = ts,
                    )
                },
            )
            templateId
        }

    suspend fun applyTemplate(templateId: String, slot: MealSlot, epochDay: Long): Int =
        withContext(io) {
            val items = itemDao.forTemplate(templateId)
            val ts = now()
            items.forEach { item ->
                foodLogDao.upsert(
                    FoodLogEntity(
                        id = newId(),
                        epochDay = epochDay,
                        mealSlot = slot,
                        foodId = item.foodId,
                        nameSnapshot = item.nameSnapshot,
                        quantityGrams = item.quantityGrams,
                        kcalSnapshot = item.kcalSnapshot,
                        proteinSnapshot = item.proteinSnapshot,
                        carbsSnapshot = item.carbsSnapshot,
                        fatSnapshot = item.fatSnapshot,
                        microsPer100Json = item.microsPer100Json,
                        // Manual e não uma origem própria: aplicar um modelo é o mesmo que
                        // registar à mão o que já lá estava, e a origem serve para explicar
                        // a falta de micronutrientes — não a via de entrada.
                        origin = LogOrigin.MANUAL,
                        // Aplicar um modelo é comer agora, e por isso leva a hora de
                        // agora — mas só se for no dia de hoje.
                        isLiquid = item.isLiquid,
                        eatenAtMin = currentMinuteOfDay().takeIf { epochDay == todayEpochDay() },
                        updatedAt = ts,
                    ),
                )
            }
            items.size
        }

    /** Devolve o modelo e os itens que foram com ele. A ordem é a inversa de os apagar. */
    suspend fun restoreTemplate(templateId: String) = withContext(io) {
        val ts = now()
        templateDao.restore(templateId, ts)
        itemDao.forTemplateForWrite(templateId).forEach { itemDao.restore(it.id, ts) }
    }

    suspend fun deleteTemplate(templateId: String) = withContext(io) {
        val ts = now()
        // Os itens primeiro: não há chave estrangeira a apagá-los em cascata, e ficariam
        // órfãos a ocupar espaço sem nada que lhes chegue.
        itemDao.forTemplate(templateId).forEach { itemDao.softDelete(it.id, ts) }
        templateDao.softDelete(templateId, ts)
    }
}

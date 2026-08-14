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

/**
 * Refeições guardadas para repetir. Um modelo é uma cópia congelada de um dia: guardar
 * copia os registos para o modelo, aplicar copia-os de volta para outro dia. Em nenhum dos
 * sentidos se vai buscar nada ao catálogo — é o que faz o modelo dar sempre o mesmo
 * resultado, mesmo que o alimento mude ou seja apagado.
 */
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
                        isLiquid = item.isLiquid,
                        updatedAt = ts,
                    ),
                )
            }
            items.size
        }

    suspend fun deleteTemplate(templateId: String) = withContext(io) {
        val ts = now()
        // Os itens primeiro: não há chave estrangeira a apagá-los em cascata, e ficariam
        // órfãos a ocupar espaço sem nada que lhes chegue.
        itemDao.forTemplate(templateId).forEach { itemDao.softDelete(it.id, ts) }
        templateDao.softDelete(templateId, ts)
    }
}

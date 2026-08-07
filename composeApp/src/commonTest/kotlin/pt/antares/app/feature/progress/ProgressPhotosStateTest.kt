package pt.antares.app.feature.progress

import pt.antares.app.core.database.entities.ProgressPhotoEntity
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgressPhotosStateTest {

    private fun foto(dia: Long) = ProgressPhotoEntity(
        id = "f$dia",
        epochDay = dia,
        localPath = "/dados/$dia.jpg",
        createdAt = 0L,
    )

    @Test
    fun `sem fotos nao ha comparacao`() {
        assertFalse(ProgressPhotosState().canCompare)
    }

    @Test
    fun `uma foto so nao e um antes-e-depois`() {
        assertFalse(ProgressPhotosState(photos = listOf(foto(20_000))).canCompare)
    }

    @Test
    fun `duas fotos do mesmo dia nao sao um antes-e-depois`() {
        val mesmoDia = listOf(foto(20_000), foto(20_000).copy(id = "outra"))
        assertFalse(ProgressPhotosState(photos = mesmoDia).canCompare)
    }

    @Test
    fun `dois dias diferentes ja contam uma historia`() {
        val duas = listOf(foto(20_000), foto(20_060))
        assertTrue(ProgressPhotosState(photos = duas).canCompare)
    }

    @Test
    fun `com muitas fotos comparam-se as pontas`() {

        val muitas = (0 until 10).map { foto(20_000 + it * 30L) }
        val estado = ProgressPhotosState(photos = muitas)
        assertTrue(estado.canCompare)
        assertTrue(estado.photos.first().epochDay < estado.photos.last().epochDay)
    }
}

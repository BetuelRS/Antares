package pt.antares.app.core.util

import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AppResultTest {

    @Test
    fun `NoSuchElementException mapeia para NotFound`() {
        assertIs<AppError.NotFound>(NoSuchElementException("food not found").toAppError())
    }

    @Test
    fun `SecurityException mapeia para Unauthorized`() {
        assertIs<AppError.Unauthorized>(SecurityException("no permission").toAppError())
    }

    @Test
    fun `SerializationException mapeia para Parsing`() {
        val error = SerializationException("bad json").toAppError()
        assertIs<AppError.Parsing>(error)
        assertEquals("bad json", (error as AppError.Parsing).message)
    }

    @Test
    fun `IllegalStateException com quota mapeia para QuotaExceeded`() {
        assertIs<AppError.QuotaExceeded>(IllegalStateException("monthly quota exceeded").toAppError())
    }

    @Test
    fun `excecao generica mapeia para Unknown`() {
        assertIs<AppError.Unknown>(RuntimeException("wat").toAppError())
    }

    @Test
    fun `runCatchingAppResult devolve Success sem excecao`() = kotlinx.coroutines.test.runTest {
        val result = runCatchingAppResult { 42 }
        assertIs<AppResult.Success<Int>>(result)
        assertEquals(42, result.value)
    }

    @Test
    fun `runCatchingAppResult devolve Failure com excecao`() = kotlinx.coroutines.test.runTest {
        val result = runCatchingAppResult { throw NoSuchElementException() }
        assertIs<AppResult.Failure>(result)
        assertIs<AppError.NotFound>(result.error)
    }
}

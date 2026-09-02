package pt.antares.app.navigation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutesTest {

    @Test
    fun `Today serializa e desserializa`() {
        val json = Json.encodeToString(Route.Today as Route)
        val decoded = Json.decodeFromString<Route>(json)
        assertEquals(Route.Today, decoded)
    }

    /**
     * A corrida **não** está aqui, e é de propósito: saiu da barra na 2.20.1 e passou a viver
     * dentro do treino. Um teste que a repusesse desfazia essa decisão em silêncio.
     */
    @Test
    fun `bottomBarRoutes tem 5 destinos na ordem certa`() {
        assertEquals(
            listOf(Route.Today, Route.Diary, Route.Workout, Route.Progresso, Route.Mais),
            bottomBarRoutes,
        )
    }
}

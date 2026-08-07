package pt.antares.app.feature.about

import com.antares.app.BuildConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppChangelogTest {

    @Test
    fun `a versao do changelog e a versao do build`() {
        assertEquals(
            BuildConfig.VERSION_NAME,
            AppChangelog.CURRENT,
            "AppChangelog.CURRENT está desatualizado face ao versionName do build.gradle.kts",
        )
    }

    @Test
    fun `a primeira entrada da lista e a versao atual`() {
        assertEquals(
            AppChangelog.CURRENT,
            AppChangelog.versions.first().name,
            "a lista tem de abrir com a versão atual — o ecrã mostra-a em destaque",
        )
    }

    @Test
    fun `nenhuma versao vem vazia ou repetida`() {
        val names = AppChangelog.versions.map { it.name }
        assertEquals(names.size, names.toSet().size, "há versões repetidas: $names")
        for (v in AppChangelog.versions) {
            assertTrue(v.title.isNotBlank(), "${v.name} sem título")
            assertTrue(v.highlights.isNotEmpty(), "${v.name} sem novidades listadas")
        }
    }

    @Test
    fun `cada versao existe nas duas linguas`() {
        for (v in AppChangelog.versions) {
            assertTrue(v.titleEn.isNotBlank(), "${v.name} sem título em inglês")
            assertTrue(v.highlightsEn.isNotEmpty(), "${v.name} sem novidades em inglês")
            assertEquals(
                v.highlights.size,
                v.highlightsEn.size,
                "${v.name}: ${v.highlights.size} novidades em PT e ${v.highlightsEn.size} em EN",
            )
            for (linha in v.highlightsEn) {
                assertTrue(linha.isNotBlank(), "${v.name} tem uma novidade vazia em inglês")
            }
        }
    }

    @Test
    fun `o changelog nao volta a crescer sem limite`() {
        assertTrue(
            AppChangelog.versions.size <= 12,
            "o changelog tem ${AppChangelog.versions.size} versões (teto: 12). " +
                "O histórico completo é trabalho do git log.",
        )
    }
}

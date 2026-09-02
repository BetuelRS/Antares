package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O ecrã dos destinos só serve para alguma coisa se estiver completo.
 *
 * Uma lista que não acompanha o código é pior do que não haver lista: quem a lê fica
 * convencido de que sabe tudo o que sai. Este teste percorre os endereços escritos no código
 * e exige uma linha no ecrã para cada um.
 *
 * A documentação em `docs/explicacao/privacidade.md` descrevia cinco destinos e a app não
 * mostrava nenhum — quem usa a app não lê o repositório. É esse o buraco que a 2.2.0 fechou.
 */
class DestinosDeclaradosTest {

    private val fontes = File("src/commonMain/kotlin")
        .walkTopDown()
        .filter { it.extension == "kt" }
        .toList()

    // Os dois ficheiros juntos: a lista vive à parte do ecrã que a desenha, e o que aqui se
    // guarda é o conteúdo, não onde ele está arrumado.
    private val ecra = listOf("Destino.kt", "DestinosScreen.kt").joinToString("\n") { nome ->
        File("src/commonMain/kotlin/pt/antares/app/feature/privacidade/" + nome).readText()
    }

    /**
     * Os anfitriões que a app contacta, e a linha do ecrã que cada um obriga a existir. Um
     * endereço novo no código sem entrada aqui faz o primeiro teste falhar — e a decisão
     * fica a ser tomada por quem o acrescentou, que é o objetivo.
     */
    private val conhecidos = mapOf(
        "world.openfoodfacts.org" to "outgoing_off_title",
        "tiles.openfreemap.org" to "outgoing_map_title",
        "raw.githubusercontent.com" to "outgoing_images_title",
        "supabase.co" to "outgoing_ai_title",

        // Passou a ser um destino a sério na 2.7.0: é de lá que vem o catálogo que se
        // atualiza. Até aí, o teste ignorava-o — havia ligações para o GitHub em comentários
        // e em texto de licenças, e nenhuma delas era um pedido.
        "github.com" to "outgoing_catalogo_title",
    )

    // Endereços que aparecem no código sem nunca serem visitados: espaços de nomes de XML,
    // ligações de licenças em texto, documentação. Cada um está aqui com a sua razão.
    private val naoSaoPedidos = listOf(
        "schemas.android.com",
        "developer.android.com",
        "creativecommons.org",
        "opendatacommons.org",
        "gnu.org",
        "www.topografix.com",
    )

    @Test
    fun `nenhum endereco novo entrou sem linha no ecra`() {
        val enderecos = Regex("""https?://([a-z0-9.\-]+)""")
            .findAll(fontes.joinToString("\n") { it.readText() })
            .map { it.groupValues[1] }
            .filterNot { endereco -> naoSaoPedidos.any { endereco.endsWith(it) } }
            .filterNot { it.endsWith("openfoodfacts.org") && it != "world.openfoodfacts.org" }
            .distinct()
            .toList()

        val semLinha = enderecos.filter { endereco ->
            conhecidos.keys.none { endereco.endsWith(it) }
        }
        assertTrue(
            semLinha.isEmpty(),
            "a app contacta $semLinha e o ecrã dos destinos não o diz. Acrescenta a linha, " +
                "ou acrescenta o endereço à lista deste teste com a razão.",
        )
    }

    @Test
    fun `cada destino conhecido tem a sua linha`() {
        val emFalta = conhecidos.filterValues { chave -> !ecra.contains(chave) }
        assertTrue(emFalta.isEmpty(), "sumiram linhas do ecrã: ${emFalta.keys}")
    }

    @Test
    fun `cada linha diz o que vai e quando`() {

        // Um nome de serviço sozinho não informa ninguém. As três partes são o que torna a
        // lista verificável: quem, o quê, quando.
        for (chave in conhecidos.values) {
            val base = chave.removeSuffix("_title")
            assertTrue(ecra.contains(base + "_what"), "$base ficou sem «o que vai»")
            assertTrue(ecra.contains(base + "_when"), "$base ficou sem «quando»")
        }
    }

    @Test
    fun `a copia de seguranca esta na lista, e separada dos destinos de rede`() {

        // Não sai para a Internet, e por isso não pode estar na mesma secção — mas sai do
        // que só a app conseguia ler, e desde a 2.1.0 acontece sozinha. É a linha desta
        // lista que mais gente vai desconhecer.
        assertTrue(ecra.contains("outgoing_backup_title"), "a cópia automática não está na lista")
        assertTrue(
            ecra.contains("DESTINOS_NO_APARELHO"),
            "a cópia deixou de estar separada dos destinos de rede",
        )
    }

    @Test
    fun `o ecra esta ao alcance do menu`() {
        val menu = File(
            "src/commonMain/kotlin/pt/antares/app/feature/me/AppMenuScreen.kt",
        ).readText()
        val rotas = File("src/commonMain/kotlin/pt/antares/app/navigation")
            .walkTopDown()
            .filter { it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        assertTrue(menu.contains("app.destinos"), "o ecrã saiu do menu")
        assertTrue(
            rotas.contains("composable<Route.Destinos>"),
            "a rota do ecrã não leva a lado nenhum",
        )
    }

    @Test
    fun `a lista de rede tem os seis destinos`() {

        // Contados e não estimados: a secção nasceu com cinco, ganhou o catálogo na 2.7.0, e
        // um desaparecimento silencioso é exatamente o que este ficheiro existe para impedir.
        val bloco = ecra
            .substringAfter("val DESTINOS_DE_REDE")
            .substringBefore("val DESTINOS_NO_APARELHO")
        assertEquals(
            6,
            Regex("""Destino\(""").findAll(bloco).count(),
            "a secção dos destinos de rede deixou de ter seis linhas",
        )
    }
}

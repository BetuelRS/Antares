package pt.antares.app.feature.privacidade

import org.jetbrains.compose.resources.StringResource
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.outgoing_ai_title
import pt.antares.app.generated.resources.outgoing_ai_what
import pt.antares.app.generated.resources.outgoing_ai_when
import pt.antares.app.generated.resources.outgoing_backup_title
import pt.antares.app.generated.resources.outgoing_backup_what
import pt.antares.app.generated.resources.outgoing_backup_when
import pt.antares.app.generated.resources.outgoing_delete_title
import pt.antares.app.generated.resources.outgoing_delete_what
import pt.antares.app.generated.resources.outgoing_delete_when
import pt.antares.app.generated.resources.outgoing_images_title
import pt.antares.app.generated.resources.outgoing_images_what
import pt.antares.app.generated.resources.outgoing_images_when
import pt.antares.app.generated.resources.outgoing_map_title
import pt.antares.app.generated.resources.outgoing_map_what
import pt.antares.app.generated.resources.outgoing_map_when
import pt.antares.app.generated.resources.outgoing_off_title
import pt.antares.app.generated.resources.outgoing_off_what
import pt.antares.app.generated.resources.outgoing_off_when

/**
 * Um destino: o que lá vai e quando.
 *
 * Escrito como dados e não como composições soltas para o `DestinosDeclaradosTest` os poder
 * contar — a lista tem de cobrir todos os sítios para onde a app abre uma ligação, e uma
 * ligação nova sem linha aqui é a app a esconder-se.
 */
data class Destino(
    val titulo: StringResource,
    val oQueVai: StringResource,
    val quando: StringResource,
)

/**
 * Os cinco destinos fora do telemóvel. A ordem é a da probabilidade de acontecerem sem
 * ninguém pedir: a procura é diária, o apagamento é uma vez na vida.
 */
val DESTINOS_DE_REDE: List<Destino> = listOf(
    Destino(
        Res.string.outgoing_off_title,
        Res.string.outgoing_off_what,
        Res.string.outgoing_off_when,
    ),
    Destino(
        Res.string.outgoing_ai_title,
        Res.string.outgoing_ai_what,
        Res.string.outgoing_ai_when,
    ),
    Destino(
        Res.string.outgoing_map_title,
        Res.string.outgoing_map_what,
        Res.string.outgoing_map_when,
    ),
    Destino(
        Res.string.outgoing_images_title,
        Res.string.outgoing_images_what,
        Res.string.outgoing_images_when,
    ),
    Destino(
        Res.string.outgoing_delete_title,
        Res.string.outgoing_delete_what,
        Res.string.outgoing_delete_when,
    ),
)

/**
 * A cópia de segurança não é um destino de rede, e por isso não está com os outros: nada
 * dela sai para a Internet. Mas sai do que só a app conseguia ler, e desde a 2.1.0 acontece
 * sozinha — o que a torna a linha desta lista que mais gente vai desconhecer.
 */
val DESTINOS_NO_APARELHO: List<Destino> = listOf(
    Destino(
        Res.string.outgoing_backup_title,
        Res.string.outgoing_backup_what,
        Res.string.outgoing_backup_when,
    ),
)

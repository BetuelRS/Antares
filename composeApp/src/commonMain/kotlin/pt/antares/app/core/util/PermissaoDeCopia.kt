package pt.antares.app.core.util

import androidx.compose.runtime.Composable

/**
 * Pede a permissão de escrita que só o Android 9 e anteriores precisam para a app poder
 * gravar em `Documentos/Antares`. Devolve nulo quando não há nada a pedir — que é o caso da
 * esmagadora maioria dos telemóveis, onde a app escreve pelo MediaStore.
 *
 * Nulo e não uma função que não faz nada: assim o ecrã pode esconder o botão em vez de
 * mostrar um que não faz nada quando se carrega nele.
 */
@Composable
expect fun rememberPedidoDePastaDeCopias(onResult: (concedida: Boolean) -> Unit): (() -> Unit)?

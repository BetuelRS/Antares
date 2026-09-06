package pt.antares.app.core.designsystem

import androidx.compose.runtime.Composable

/**
 * Trava o gesto de voltar do sistema enquanto `activo` for verdade.
 *
 * Existe por causa do cadeado da corrida: ele desactivava os botões do ecrã e deixava o recuo
 * do sistema passar, o que é meio cadeado — quem o fecha para pôr o telemóvel no bolso não
 * quer que um gesto acidental saia da corrida.
 *
 * `expect`/`actual` e não o `BackHandler` do Compose Multiplatform: nesta versão (1.8.1) ele
 * vive num artefacto que não está no caminho de compilação comum, e o do `activity-compose` é
 * de Android. Um sítio só, para o dia em que isso mude ser uma linha.
 */
@Composable
expect fun TravarRecuo(activo: Boolean)

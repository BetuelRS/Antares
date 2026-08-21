package pt.antares.app.core.designsystem.motion

import androidx.compose.runtime.Composable

/**
 * Se o sistema quer animações.
 *
 * Quem desliga as animações no Android não o faz por gosto: faz por enjoo com movimento, por
 * bateria, ou porque num telemóvel lento cada animação é meio segundo de espera. Uma app que
 * ignora essa definição está a decidir por essa pessoa.
 *
 * É a diferença entre um sistema de movimento bem feito e um catálogo de efeitos.
 */
@Composable
expect fun animacoesLigadas(): Boolean

package pt.antares.app.feature.today

/**
 * Os destinos do «Hoje», num sítio só.
 *
 * São oito lambdas de navegação, e é a mesma forma que o `MenuDoTreino` e os `DestinosDoCorpo`
 * já usam: passá-las soltas eram onze parâmetros por posição, e trocar duas do mesmo tipo não
 * dá erro de compilação nenhum — dá um cartão que abre o ecrã errado.
 */
class DestinosDoHoje(
    val peso: () -> Unit,
    val refeicao: () -> Unit,
    val treino: () -> Unit,
    val jejum: () -> Unit,
    val corrida: () -> Unit,
    val treinador: () -> Unit,
    val perfil: () -> Unit,

    /** Sem perfil não há metas, e este é o caminho para as respostas que faltam. */
    val arranque: () -> Unit,
)

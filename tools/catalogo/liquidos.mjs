/**
 * Quem esta marcado como liquido, e quem devia estar.
 *
 * A lista de liquidos veio de uma extraccao antiga da app e esta errada dos dois lados: traz
 * solidos que passaram por agua, e falta-lhe a maior parte dos oleos.
 *
 * **O lado dos solidos corrige-se sozinho** — um nome que diga «cozido em agua» ou «em po»
 * nao e liquido, e isso le-se do nome com certeza.
 *
 * **O lado que falta nao se corrige sozinho, e e de proposito.** Tentei tres regras cada vez
 * mais apertadas e as tres marcaram comida solida: «Olive, black, in oil», «Gherkin, pickled
 * in vinegar», «Milk chocolate, bar». O nome ingles e a armadilha — uma barra de chocolate de
 * leite comeca por «Milk». Marcar um solido como liquido nao rebenta nada: so poe a app a
 * oferecer mililitros para uma azeitona, e ninguem da por isso ate estar publicado.
 *
 * Por isso os candidatos vao para uma fila, como as outras suspeitas do bloco D. Uma pessoa
 * decide, e a decisao fica escrita em `correcoes.json` com as outras.
 */

/**
 * O nome diz que isto e o proprio liquido — e nao alguma coisa que o leva dentro.
 *
 * A ancora no inicio e o que separa «Óleo de girassol» de «Azeitona em óleo». Nao chega,
 * como o cabecalho explica, e por isso o que sai daqui e uma **sugestao**.
 */
const CANDIDATOS = [
  /^(azeite|óleo|oleo)(?![\p{L}])/iu,
  /^oil,/iu,
  /^sumo(?![\p{L}])/iu,
  /^leite(?!\s+em\s+pó)(?![\p{L}])/iu,
  /^água(?![\p{L}])/iu,
  /^cerveja(?![\p{L}])/iu,
  /^vinho(?![\p{L}])/iu,
  /^vinagre(?![\p{L}])/iu,
];

/** Gorduras solidas a temperatura ambiente: tem nome de oleo e nao se medem ao copo. */
const SOLIDAS_APESAR_DO_NOME = [/coco(?![\p{L}])|coconut|palma|palm|shea|karité|manteiga|butter/iu];

/**
 * Os alimentos que parecem liquidos e nao estao marcados.
 *
 * Le **so o nome portugues**: o ingles produziu os piores enganos, porque «Milk chocolate» e
 * «Oil, olive» comecam pela palavra que interessa sem serem o que ela diz.
 */
export function candidatosALiquido(alimentos, pareceSolido) {
  return alimentos
    .filter((a) => !a.isLiquid)
    .filter((a) => !pareceSolido(a))
    .filter((a) => !SOLIDAS_APESAR_DO_NOME.some((re) => re.test(a.namePt)))
    .filter((a) => CANDIDATOS.some((re) => re.test(a.namePt)))
    .map((a) => ({ id: a.id, nome: a.namePt, familia: a.familia ?? null }));
}

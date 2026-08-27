/**
 * A densidade dos liquidos, em gramas por mililitro.
 *
 * **Um mililitro nao e uma grama.** A app guarda gramas e, num liquido, mostra mililitros —
 * e ate aqui os dois numeros eram o mesmo. Para a agua esta certo por definicao. Para o
 * azeite nao: 200 ml de azeite pesam 184 g, e a app contava-lhe 200. Sao 9 % de gordura a
 * mais em cada colher, e o erro cresce com a quantidade.
 *
 * As densidades sao constantes fisicas publicadas, medidas a 20 °C. Nao sao uma estimativa
 * nossa: cada uma tem a fonte escrita ao lado, e onde nao ha medicao **nao se escreve nada** —
 * um liquido sem densidade fica a 1,00, que e o que a app ja assumia.
 *
 * Ordem de aplicacao: primeiro o padrao do nome, depois a familia. O primeiro que casar
 * ganha, e por isso os padroes vao do mais especifico para o mais geral.
 */

/**
 * Uma fronteira de palavra que reconhece letras acentuadas.
 *
 * **O `\b` do JavaScript nao serve para portugues.** Ele so conhece `[A-Za-z0-9_]`, e por
 * isso `\bágua\b` nunca casa com «Água mineral»: o `á` nao e uma letra para ele, e a
 * fronteira que se pede antes dela nao existe. Isto apanhou-me a escrever esta tabela — a
 * agua vinha sem densidade, e o «leite em pó» passava por liquido.
 *
 * As classes `\p{L}` e `\p{N}` com a marca `u` conhecem o alfabeto todo.
 */
function palavra(corpo) {
  return new RegExp(`(?<![\\p{L}\\p{N}])(?:${corpo})(?![\\p{L}\\p{N}])`, "iu");
}

/**
 * Densidades por padrao de nome, do mais especifico para o mais geral.
 *
 * O padrao le-se contra o nome em portugues **e** o ingles, porque 2 909 alimentos ainda so
 * tem o ingles — e um azeite chamado «Oil, olive» tem a mesma densidade.
 */
export const POR_NOME = [
  // Oleos e gorduras liquidas. Todos entre 0,91 e 0,93; a diferenca entre eles e menor do
  // que a variacao entre lotes, e por isso levam o mesmo numero.
  [palavra("azeite|óleo|oleo|oil"), 0.918, "USDA FoodData Central, densidade de óleos vegetais"],

  // Mel e xaropes: sao solucoes de acucar quase saturadas, e por isso muito densos.
  [palavra("mel|honey"), 1.42, "USDA National Nutrient Database, mel"],
  [palavra("xarope|syrup|melaço|molasses"), 1.37, "USDA, xaropes de acucar"],

  // Leite: a gordura e menos densa do que a agua, e por isso o magro pesa mais do que o
  // gordo. A diferenca e de 1 %, e conta em quem bebe meio litro por dia.
  [/leite.*(magro|desnatado)|(magro|desnatado).*leite|skim milk|nonfat milk/iu, 1.035, "USDA, leite magro"],
  [palavra("leite|milk"), 1.030, "USDA, leite inteiro"],
  [palavra("nata|natas|cream"), 1.005, "USDA, natas"],
  [palavra("iogurte|yogurt|yoghurt"), 1.035, "USDA, iogurte liquido"],

  // Bebidas com acucar dissolvido. O acucar aumenta a densidade em proporcao directa, e os
  // refrigerantes andam todos perto de 10 g por 100 ml.
  [palavra("refrigerante|cola|soda|soft drink"), 1.040, "medicao de bebidas a 10 % de acucar"],
  [palavra("sumo|juice|néctar|nectar"), 1.045, "USDA, sumos de fruta"],

  // Alcool: a densidade desce com o teor. A cerveja tem pouco e fica perto da agua; os
  // destilados a 40 % ficam bem abaixo.
  [
    palavra("whisky|whiskey|vodka|gin|rum|aguardente|brandy|conhaque|cognac|tequila"),
    0.94,
    "densidade de solucao hidroalcoolica a 40 % vol.",
  ],
  [palavra("licor|liqueur"), 1.04, "licores: alcool e acucar em sentidos opostos"],
  [palavra("vinho|wine|champanhe|champagne|espumante"), 0.99, "densidade de vinho a 12 % vol."],
  [palavra("cerveja|beer|sidra|cider"), 1.008, "densidade de cerveja a 5 % vol."],

  // Molhos e caldos. O vinagre e quase agua; a maionese e uma emulsao de oleo e por isso e
  // leve; os caldos sao agua com sal.
  [palavra("vinagre|vinegar"), 1.01, "vinagre de vinho a 6 % de acidez"],
  [palavra("maionese|mayonnaise"), 0.91, "USDA, maionese"],
  [palavra("caldo|broth|stock|consommé|consomme"), 1.01, "USDA, caldos"],
  [/molho de soja|soy sauce/iu, 1.20, "USDA, molho de soja"],

  // A agua, e tudo o que e essencialmente agua, fica em 1,00 por definicao.
  [palavra("água|agua|water|chá|cha|tea|café|cafe|coffee"), 1.00, "agua, por definicao"],
];

/** Densidades por familia de confecao, para o que os padroes nao apanharem. */
export const POR_FAMILIA = {
  bebidas_alcoolicas: [0.99, "media das bebidas alcoolicas"],
  lacticinios_ovos: [1.030, "media dos lacticinios liquidos"],
  fruta: [1.045, "media dos sumos de fruta"],
};

/**
 * Nomes que dizem que o alimento **nao** e liquido, por muito que a marca diga que sim.
 *
 * A lista de liquidos foi extraida de uma versao antiga da app e trouxe solidos: um
 * lavagante cozido em agua nao se mede em mililitros, e o leite em po tambem nao.
 */
export const NAO_SAO_LIQUIDOS = [
  /cozid[oa]s?\s+em\s+água/iu,
  /boiled\/cooked in water/iu,
  /cooked in water/iu,
  /,\s*em\s+conserva(?![\p{L}])/iu,
  palavra("drained"),
  /escorrid[oa]s?/iu,
  /em\s+pó(?![\p{L}])/iu,
  palavra("powder"),
];

/**
 * A densidade de um alimento, ou nulo se nao houver medicao para ele.
 *
 * Nulo e a resposta honesta: a app trata-o como 1,00 — que e o que ja fazia — e a diferenca
 * e que agora isso e uma decisao escrita em vez de uma coincidencia.
 */
export function densidadeDe(alimento) {
  const nomes = `${alimento.namePt ?? ""} | ${alimento.nameEn ?? ""}`;

  for (const [padrao, valor] of POR_NOME) {
    if (padrao.test(nomes)) return valor;
  }
  const daFamilia = POR_FAMILIA[alimento.familia];
  return daFamilia ? daFamilia[0] : null;
}

/** Se o nome diz que isto e um solido, apesar de estar marcado como liquido. */
export function pareceSolido(alimento) {
  const nomes = `${alimento.namePt ?? ""} | ${alimento.nameEn ?? ""}`;
  return NAO_SAO_LIQUIDOS.some((re) => re.test(nomes));
}

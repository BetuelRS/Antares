/**
 * As porções domésticas: «uma fatia», «uma chávena», «uma unidade média».
 *
 *     node tools/porcoes/construir.mjs
 *
 * Escreve `porcoes.json`, que o `catalogo/construir.mjs` aplica. Nenhuma das quatro fontes do
 * catálogo publica porções — a CIQUAL, a TCA e o USDA SR são tabelas por 100 g —, e por isso
 * **registar era escrever gramas à mão para 96% do catálogo**.
 *
 * A fonte é a tabela de porções do FoodData Central, e a junção é pelo **identificador**: os
 * alimentos `usda-*` do catálogo são registos do SR Legacy, e o número que trazem no nome é o
 * `fdc_id`. Não há correspondência por nome nenhuma aqui, e é isso que torna estas porções
 * exactas em vez de prováveis.
 *
 * **O que não é porção não entra.** Metade das linhas da tabela são conversões de unidade —
 * uma onça, uma libra, uma polegada cúbica — que não são a maneira de ninguém comer, e muito
 * menos em Portugal. Uma lista onde «1 oz» aparece ao lado de «1 fatia» é uma lista que se
 * fecha sem escolher nada.
 */
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import { lerCsv } from "../confecao/tabelas.mjs";
import { lerVocabulario, traduzirNome } from "../vocabulario/traduzir.mjs";

const HERE = dirname(fileURLToPath(import.meta.url));
const RAIZ = join(HERE, "..", "..");
const DADOS = join(HERE, "data");
const SAIDA = join(HERE, "porcoes.json");

/**
 * As unidades imperiais e as medidas de laboratório. Não são porções: são o mesmo peso dito
 * de outra maneira, e em Portugal não são sequer a maneira de o dizer.
 */
const NAO_SAO_PORCOES = /^(oz|fl oz|lb|lbs|cubic inch|quart|pint|gallon|gram|grams|g|kg|ml|liter|litre)\b/i;

/**
 * As descrições que não descrevem nada. «serving» sem mais nada é «uma dose», que não diz
 * quanto é — e a tabela já dá o peso, portanto a linha não se perde: perde-se o rótulo.
 */
const SEM_ROTULO = /^(serving|servings|portion|piece|pieces|unit|units)$/i;

// Um quilo. Acima disto e um assado inteiro, uma tarte inteira ou um ganso inteiro.
const MAXIMO_DE_UMA_PORCAO_G = 1000;

const linhas = lerCsv(readFileSync(join(DADOS, "food_portion.csv"), "utf8")).slice(1);
const vocabulario = lerVocabulario();

const porAlimento = new Map();
let semRotulo = 0;
let naoSaoPorcoes = 0;
let porTraduzir = 0;
let grandesDemais = 0;

for (const l of linhas) {
  if (l.length < 8) continue;
  const [, fdc, , quantidade, , descricao, modificador, gramas] = l;

  const peso = Number(gramas);
  if (!Number.isFinite(peso) || peso <= 0) continue;

  // Um ganso inteiro pesa 2 638 g e a tabela chama-lhe «1 goose». É uma unidade legítima e
  // não é uma porção: ninguém come um ganso de uma vez, e um atalho que o proponha é ruído
  // na única linha do ecrã onde a pessoa está a decidir depressa.
  if (peso > MAXIMO_DE_UMA_PORCAO_G) { grandesDemais++; continue; }

  const bruto = String(modificador || descricao || "").trim();
  if (!bruto) continue;
  if (NAO_SAO_PORCOES.test(bruto)) { naoSaoPorcoes++; continue; }
  if (SEM_ROTULO.test(bruto)) { semRotulo++; continue; }

  const traduzido = traduzirNome(bruto, vocabulario);
  if (!traduzido.completo) { porTraduzir++; continue; }

  // A quantidade da tabela é quase sempre 1. Quando não é — «2 cookies» —, o peso é o das
  // duas, e a porção que interessa é a de uma.
  const quantas = Number(quantidade) || 1;

  const id = `usda-${fdc}`;
  if (!porAlimento.has(id)) porAlimento.set(id, []);
  porAlimento.get(id).push({
    nome: traduzido.nome,
    gramas: Math.round((peso / quantas) * 10) / 10,
  });
}

/**
 * Ordena as porções de cada alimento e tira as repetidas.
 *
 * A primeira é a que a app propõe. **A mais pesada não é a melhor**, e a mais leve também
 * não: o que se quer à frente é a que uma pessoa come de uma vez, e essa é a que está mais
 * perto de cem gramas — o resto são colheres e chávenas, que servem para medir e não para
 * descrever uma refeição.
 */
const PORCAO_TIPICA_G = 100;

// Quatro chegam para uma fila de botões. Mais do que isso é um menu, e um menu numa linha
// de atalhos é mais lento do que escrever o número.
const MAXIMO_POR_ALIMENTO = 4;

const saida = {};
for (const [id, lista] of [...porAlimento.entries()].sort((a, b) => (a[0] < b[0] ? -1 : 1))) {
  const vistas = new Set();
  const unicas = lista.filter((p) => {
    const chave = `${p.nome}:${p.gramas}`;
    if (vistas.has(chave)) return false;
    vistas.add(chave);
    return true;
  });

  unicas.sort((a, b) =>
    Math.abs(a.gramas - PORCAO_TIPICA_G) - Math.abs(b.gramas - PORCAO_TIPICA_G) ||
    (a.nome < b.nome ? -1 : a.nome > b.nome ? 1 : 0));

  saida[id] = unicas.slice(0, MAXIMO_POR_ALIMENTO);
}



writeFileSync(SAIDA, JSON.stringify(saida, null, 2) + "\n");

const total = Object.values(saida).reduce((s, l) => s + l.length, 0);
console.log(`porções: ${Object.keys(saida).length} alimentos, ${total} porções`);
console.log(`  linhas que não são porções (onças, libras): ${naoSaoPorcoes}`);
console.log(`  linhas sem rótulo útil («serving»):         ${semRotulo}`);
console.log(`  rótulos que o vocabulário não traduz:       ${porTraduzir}`);
console.log(`  pesos grandes de mais para uma porção:      ${grandesDemais}`);
console.log(`  → ${SAIDA}`);

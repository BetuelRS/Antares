/**
 * As duas tabelas do USDA que dizem o que acontece à comida quando se cozinha.
 *
 * Ambas de domínio público, e ambas descarregadas para `tools/confecao/data/`:
 *
 * - **Table of Nutrient Retention Factors, Release 6** — 7 018 linhas, 270 preparações em 13
 *   grupos de alimento, 26 nutrientes. Diz **quanto de cada nutriente sobrevive**.
 * - **Table of Cooking Yields for Meat and Poultry, Release 2** — 173 linhas, só carne e aves.
 *   Diz **quanto peso se ganha ou se perde**.
 *
 * **A assimetria entre as duas é o que decide o desenho desta versão.** A retenção cobre
 * legumes, ovos, leite e cereais; o rendimento não. E sem rendimento não há como passar de
 * «por 100 g de cru» para «por 100 g de cozinhado» — é a divisão que a fórmula do USDA faz:
 *
 *     nutriente por 100 g cozinhado = nutriente por 100 g cru × retenção ÷ rendimento
 *
 * Sem essa divisão, cozer espinafres fazia-os perder ferro **e** massa sem ninguém contar a
 * segunda coisa, e o resultado ficava mais errado do que não fazer nada.
 */
import { readFileSync } from "node:fs";
import { join } from "node:path";

/** Um leitor de CSV que percebe aspas e vírgulas dentro delas. As duas tabelas têm as duas. */
export function lerCsv(texto) {
  const linhas = [];
  let campo = "";
  let atual = [];
  let entreAspas = false;

  for (let i = 0; i < texto.length; i++) {
    const c = texto[i];
    if (entreAspas) {
      if (c === '"' && texto[i + 1] === '"') { campo += '"'; i++; } else if (c === '"') entreAspas = false;
      else campo += c;
    } else if (c === '"') entreAspas = true;
    else if (c === ",") { atual.push(campo); campo = ""; } else if (c === "\n") { atual.push(campo); linhas.push(atual); atual = []; campo = ""; } else if (c !== "\r") campo += c;
  }
  if (campo || atual.length) { atual.push(campo); linhas.push(atual); }
  return linhas;
}

/**
 * O código do nutriente no USDA e a chave por que a app o conhece.
 *
 * Dezoito das nossas quarenta e duas chaves. As que ficam de fora — o selénio, o iodo, o
 * manganês, a vitamina D, a E e a K — **não estão na tabela de retenção**, e não se inventa
 * um factor para elas: um nutriente sem factor fica com o valor do cru, que é a resposta
 * honesta para «não sabemos o que lhe acontece».
 *
 * O folato entra pelo total (417) e não pelo ácido fólico (431) nem pelo folato alimentar
 * (432): a app guarda um número só, e o total é o que ele é.
 */
export const NUTRIENTE_POR_CODIGO = {
  221: "alcohol_g",
  301: "calcium_mg",
  303: "iron_mg",
  304: "magnesium_mg",
  305: "phosphorus_mg",
  306: "potassium_mg",
  307: "sodium_mg",
  309: "zinc_mg",
  312: "copper_mg",
  321: "betaCarotene_ug",
  392: "vitA_ug",
  401: "vitC_mg",
  404: "vitB1_mg",
  405: "vitB2_mg",
  406: "vitB3_mg",
  415: "vitB6_mg",
  417: "vitB9_ug",
  418: "vitB12_ug",
};

const PERCENTAGEM = 100;

/**
 * Lê a retenção e agrupa por preparação.
 *
 * A chave é o par grupo-de-alimento e descrição, porque a mesma palavra quer dizer coisas
 * diferentes conforme o que se está a cozinhar: «BOILED» num legume perde vitamina C para a
 * água, «BOILED» num ovo não perde nada — a casca é a panela.
 */
export function lerRetencao(dataDir) {
  const linhas = lerCsv(readFileSync(join(dataDir, "retencao.csv"), "utf8")).slice(1);
  const preparacoes = new Map();

  for (const l of linhas) {
    if (l.length < 6) continue;
    const [codigo, grupo, descricao, nutriente, , factor] = l;
    const chave = NUTRIENTE_POR_CODIGO[Number(nutriente)];
    if (!chave) continue;

    const id = `${grupo}:${descricao}`;
    if (!preparacoes.has(id)) {
      preparacoes.set(id, { codigo: Number(codigo), grupo, descricao, retencoes: {} });
    }
    preparacoes.get(id).retencoes[chave] = Number(factor) / PERCENTAGEM;
  }

  return [...preparacoes.values()];
}

/**
 * Lê os rendimentos e resume-os por grupo e método.
 *
 * **A mediana e não a média.** São dezassete métodos sobre 173 cortes, e um corte gordo
 * assado perde metade do peso enquanto um lombo perde um quinto; a média de dois desses põe
 * o rendimento onde nenhum dos dois está. A mediana é também o que o motor de qualidade já
 * usa, pela mesma razão.
 */
export function lerRendimentos(dataDir) {
  const linhas = lerCsv(readFileSync(join(dataDir, "rendimentos.csv"), "utf8")).slice(1);
  const porMetodo = new Map();

  for (const l of linhas) {
    if (l.length < 5) continue;
    const grupo = l[0].trim();
    const metodo = normalizarMetodo(l[3]);
    const rendimento = Number(l[4]);
    if (!metodo || !Number.isFinite(rendimento) || rendimento <= 0) continue;

    const id = `${grupo}:${metodo}`;
    if (!porMetodo.has(id)) porMetodo.set(id, { grupo, metodo, valores: [] });
    porMetodo.get(id).valores.push(rendimento / PERCENTAGEM);
  }

  return [...porMetodo.values()].map((m) => ({
    grupo: m.grupo,
    metodo: m.metodo,
    rendimento: arredondar(mediana(m.valores)),
    cortes: m.valores.length,
  }));
}

/**
 * A tabela escreve o mesmo método de cinco maneiras — «Broiled or Grilled», «Broiled or
 * grilled», «Grilled», «Broiled», «Pan-broiled» — e tratá-las como métodos diferentes dava
 * cinco rendimentos para a mesma coisa, cada um medido em meia dúzia de cortes.
 */
export function normalizarMetodo(bruto) {
  const s = String(bruto ?? "").trim().toLowerCase();
  if (!s) return null;
  if (s.includes("braised")) return "estufado";
  if (s.includes("deep fat")) return "frito";
  if (s.includes("pan-fried") || s.includes("pan fried") || s.includes("sauteed")) return "salteado";
  if (s.includes("broil") || s.includes("grill")) return "grelhado";
  if (s.includes("roast") || s.includes("baked")) return "assado";
  if (s.includes("poached") || s.includes("simmered") || s.includes("stewed")) return "cozido";
  if (s.includes("microwave")) return "micro-ondas";
  if (s.includes("browned")) return "alourado";

  // «Multiple cooking methods» é o que a tabela escreve quando não sabe qual foi. Um método
  // que não se sabe qual é não é um método.
  return null;
}

function mediana(valores) {
  const ordenados = [...valores].sort((a, b) => a - b);
  const meio = Math.floor(ordenados.length / 2);
  return ordenados.length % 2
    ? ordenados[meio]
    : (ordenados[meio - 1] + ordenados[meio]) / 2;
}

const CASAS = 1000;
const arredondar = (v) => Math.round(v * CASAS) / CASAS;

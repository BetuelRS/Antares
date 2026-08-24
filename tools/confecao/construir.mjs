/**
 * Constrói a tabela de confeção que a app transporta.
 *
 *     node tools/confecao/construir.mjs
 *
 * Junta as duas tabelas do USDA numa só, por família de alimento e método: **quanto peso
 * sobra** e **quanto de cada nutriente sobrevive**. É o que permite à app responder a «e se
 * eu cozer isto?» sem ter um alimento separado para cada estado.
 *
 * A fórmula que isto serve é a do próprio USDA:
 *
 *     nutriente por 100 g cozinhado = nutriente por 100 g cru × retenção ÷ rendimento
 *
 * A divisão pelo rendimento é a parte que se esquece. Cozer espinafres perde ferro para a
 * água **e** perde água: contar só a primeira coisa dá um valor mais errado do que não fazer
 * nada. É por isso que um método sem rendimento conhecido não se publica com um rendimento
 * inventado — publica-se sem, e a app pede o peso depois de cozinhar.
 */
import { writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import { lerRetencao, lerRendimentos } from "./tabelas.mjs";
import { FAMILIAS } from "./familias.mjs";

const HERE = dirname(fileURLToPath(import.meta.url));
const RAIZ = join(HERE, "..", "..");
const DADOS = join(HERE, "data");
const SAIDA = join(RAIZ, "composeApp", "src", "commonMain", "composeResources", "files", "confecao.json");

/**
 * A versão da tabela de confeção. Sobe à mão, como a do catálogo, e pela mesma razão: é por
 * ela que a app decide se vale a pena reler o ficheiro.
 */
const VERSAO = 1;

/**
 * Os métodos, e as palavras por que as duas tabelas os escrevem.
 *
 * A ordem é a da leitura: `BRAISED` tem de ser visto antes de `BOILED` — estufar não é cozer,
 * e o rendimento medido diz o mesmo (68 % contra 39 % no porco). `REHEATED` fica em último
 * por ser o que sobra.
 */
const METODOS = [
  { id: "estufado", nome: "Estufado", nomeEn: "Braised", padrao: /BRAIS|BRSD/ },
  { id: "grelhado", nome: "Grelhado", nomeEn: "Grilled", padrao: /BROIL|BRLD/ },
  { id: "frito", nome: "Frito", nomeEn: "Fried", padrao: /FRIED|FRYD|FRENCH FRY|HASH BROWN/ },
  { id: "salteado", nome: "Salteado", nomeEn: "Sautéed", padrao: /SAUTEED|STIR FRY|BROWN/ },
  { id: "assado", nome: "Assado", nomeEn: "Roasted", padrao: /BAKED|BKD|ROAST|TOASTED/ },
  {
    id: "cozido",
    nome: "Cozido",
    nomeEn: "Boiled",
    padrao: /BOIL|BOILD|BLD|SIMM|SIMR|POACHED|STEWED|HARD COOKED|STEAMED|COOKED W/,
  },
  { id: "reaquecido", nome: "Reaquecido", nomeEn: "Reheated", padrao: /REHEATED|HEATED/ },
];

/**
 * Onde a tabela separa «com molho» de «sem molho», qual das duas se escolhe.
 *
 * A diferença não é pequena: os minerais que saem para o líquido voltam ao prato se o líquido
 * for comido, e não voltam se for deitado fora. A regra segue o que se faz na cozinha — de um
 * estufado come-se o molho, de um bife grelhado não se bebe a gordura da grelha.
 */
const COME_SE_O_MOLHO = new Set(["estufado", "cozido"]);

const COM_MOLHO = /W[\/ ]DRIP|W\/DRIPPING/;
const SEM_MOLHO = /WO[\/ ]DRIP|WO\/DRIPPING/;

function metodoDe(descricao) {
  // As descrições combinadas — «BOILED+FRIED» — são dois métodos seguidos, e arrumá-las em
  // qualquer um dos dois dá um número que não descreve nenhum.
  if (descricao.includes("+")) return null;
  for (const m of METODOS) if (m.padrao.test(descricao)) return m.id;
  return null;
}

const retencao = lerRetencao(DADOS);
const rendimentos = lerRendimentos(DADOS);

const porGrupo = Object.fromEntries(Object.entries(FAMILIAS).map(([nome, g]) => [String(g), nome]));

// ------------------------------------------------------- a retenção, por família e método

const juntas = new Map();
let semMetodo = 0;

for (const prep of retencao) {
  const familia = porGrupo[String(Number(prep.grupo))];
  if (!familia) continue;
  const metodo = metodoDe(prep.descricao);
  if (!metodo) { semMetodo++; continue; }

  const chave = `${familia}:${metodo}`;
  if (!juntas.has(chave)) juntas.set(chave, { familia, metodo, preferidas: [], outras: [] });
  const alvo = juntas.get(chave);

  const querMolho = COME_SE_O_MOLHO.has(metodo);
  const temMolho = COM_MOLHO.test(prep.descricao) && !SEM_MOLHO.test(prep.descricao);
  const semMolho = SEM_MOLHO.test(prep.descricao);
  const preferida = (querMolho && temMolho) || (!querMolho && semMolho);

  (preferida ? alvo.preferidas : alvo.outras).push(prep.retencoes);
}

/** A mediana de cada nutriente. Uma preparação atípica não pode levar o factor com ela. */
function medianaPorChave(listas) {
  const porChave = {};
  for (const mapa of listas) {
    for (const [k, v] of Object.entries(mapa)) (porChave[k] = porChave[k] ?? []).push(v);
  }
  const saida = {};
  for (const k of Object.keys(porChave).sort()) {
    const ord = porChave[k].sort((a, b) => a - b);
    const meio = Math.floor(ord.length / 2);
    const m = ord.length % 2 ? ord[meio] : (ord[meio - 1] + ord[meio]) / 2;
    saida[k] = Math.round(m * 1000) / 1000;
  }
  return saida;
}

// -------------------------------------------------------- o rendimento, onde ele existe

/**
 * O rendimento da família e método, com duas exigências.
 *
 * **Pelo menos três cortes medidos.** A tabela publica 29 % para porco no micro-ondas a partir
 * de um corte só, que era bacon; escrever isso como «o rendimento do porco no micro-ondas»
 * era escrever um número que não se pode defender.
 *
 * Quando a família não tem cortes que cheguem, usa-se a mediana do **mesmo método em todas as
 * carnes**, e diz-se que foi isso — os assados andam entre 72 % e 82 %, e a diferença entre
 * usar o valor do bovino num borrego e não ter valor nenhum não está sequer perto.
 */
const MINIMO_DE_CORTES = 3;

function rendimentoDe(familia, metodo) {
  const grupo = String(FAMILIAS[familia]);
  const proprio = rendimentos.find((r) => r.grupo === grupo && r.metodo === metodo);
  if (proprio && proprio.cortes >= MINIMO_DE_CORTES) {
    return { rendimento: proprio.rendimento, medidoEm: proprio.cortes, deOutraCarne: false };
  }

  const doMetodo = rendimentos.filter((r) => r.metodo === metodo);
  const cortes = doMetodo.reduce((s, r) => s + r.cortes, 0);
  if (!doMetodo.length || cortes < MINIMO_DE_CORTES) return null;

  const ord = doMetodo.map((r) => r.rendimento).sort((a, b) => a - b);
  const meio = Math.floor(ord.length / 2);
  const m = ord.length % 2 ? ord[meio] : (ord[meio - 1] + ord[meio]) / 2;
  return { rendimento: Math.round(m * 1000) / 1000, medidoEm: cortes, deOutraCarne: true };
}

// -------------------------------------------------------------------------- escrever

const CARNES = new Set(["vaca", "porco", "aves", "borrego"]);

const linhas = [...juntas.values()]
  .map(({ familia, metodo, preferidas, outras }) => {
    const usadas = preferidas.length ? preferidas : outras;
    const r = CARNES.has(familia) ? rendimentoDe(familia, metodo) : null;
    return {
      familia,
      metodo,
      rendimento: r?.rendimento ?? null,
      rendimentoDeOutraCarne: r ? r.deOutraCarne : false,
      preparacoes: usadas.length,
      comMolho: COME_SE_O_MOLHO.has(metodo),
      retencoes: medianaPorChave(usadas),
    };
  })
  .sort((a, b) => (a.familia < b.familia ? -1 : a.familia > b.familia ? 1
    : a.metodo < b.metodo ? -1 : a.metodo > b.metodo ? 1 : 0));

const metodos = METODOS.map(({ id, nome, nomeEn }) => ({ id, nome, nomeEn }));

writeFileSync(SAIDA, JSON.stringify({ versao: VERSAO, metodos, linhas }));

console.log(`confecão v${VERSAO}: ${linhas.length} pares família-método, ${metodos.length} métodos`);
console.log(`  preparações da tabela sem método reconhecido: ${semMetodo}`);
console.log(`  com rendimento próprio: ${linhas.filter((l) => l.rendimento && !l.rendimentoDeOutraCarne).length}`);
console.log(`  com rendimento de outra carne: ${linhas.filter((l) => l.rendimentoDeOutraCarne).length}`);
console.log(`  sem rendimento — a app pede o peso: ${linhas.filter((l) => !l.rendimento).length}`);
console.log(`  → ${SAIDA}`);
